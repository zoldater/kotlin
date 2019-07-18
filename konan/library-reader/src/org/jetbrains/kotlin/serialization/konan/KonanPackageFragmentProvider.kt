/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.library.KonanLibraryForIde
import org.jetbrains.kotlin.konan.library.KonanLibraryMetadataLoader
import org.jetbrains.kotlin.konan.library.exportForwardDeclarations
import org.jetbrains.kotlin.konan.library.isInterop
import org.jetbrains.kotlin.library.packageFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

fun createPackageFragmentProvider(
    library: KonanLibraryForIde,
    metadataLoader: KonanLibraryMetadataLoader,
    storageManager: StorageManager,
    moduleDescriptor: ModuleDescriptor,
    configuration: DeserializationConfiguration
): PackageFragmentProvider {

    val packageFragmentNames = metadataLoader.loadModuleHeader(library).packageFragmentNameList

    val deserializedPackageFragments = createDeserializedPackageFragments(
        library, metadataLoader, packageFragmentNames, moduleDescriptor, storageManager
    )

    val syntheticPackageFragments = createSyntheticPackageFragments(
        library, deserializedPackageFragments, moduleDescriptor
    )

    val provider = PackageFragmentProviderImpl(deserializedPackageFragments + syntheticPackageFragments)

    val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

    val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(
        moduleDescriptor,
        notFoundClasses,
        KonanSerializerProtocol
    )

    val components = DeserializationComponents(
        storageManager,
        moduleDescriptor,
        configuration,
        DeserializedClassDataFinder(provider),
        annotationAndConstantLoader,
        provider,
        LocalClassifierTypeSettings.Default,
        ErrorReporter.DO_NOTHING,
        LookupTracker.DO_NOTHING,
        NullFlexibleTypeDeserializer,
        emptyList(),
        notFoundClasses,
        ContractDeserializerImpl(configuration, storageManager),
        extensionRegistryLite = KonanSerializerProtocol.extensionRegistry
    )

    for (packageFragment in deserializedPackageFragments) {
        packageFragment.initialize(components)
    }

    return provider
}

private fun createDeserializedPackageFragments(
    library: KonanLibraryForIde,
    metadataLoader: KonanLibraryMetadataLoader,
    packageFragmentNames: List<String>,
    moduleDescriptor: ModuleDescriptor,
    storageManager: StorageManager
) = packageFragmentNames.flatMap {
    val fqName = FqName(it)
    val parts = library.packageMetadataParts(it)
    parts.map { partName ->
        KonanPackageFragmentForIde(fqName, library, metadataLoader, storageManager, moduleDescriptor, partName)
    }
}

private fun createSyntheticPackageFragments(
    library: KonanLibraryForIde,
    deserializedPackageFragments: List<KonanPackageFragmentForIde>,
    moduleDescriptor: ModuleDescriptor
): List<PackageFragmentDescriptor> {

    if (!library.isInterop) return emptyList()

    val mainPackageFqName = library.packageFqName?.let { FqName(it) }
        ?: error("Inconsistent manifest: interop library ${library.libraryName} should have `package` specified")
    val exportForwardDeclarations = library.exportForwardDeclarations

    val aliasedPackageFragments = deserializedPackageFragments.filter { it.fqName == mainPackageFqName }

    val result = mutableListOf<PackageFragmentDescriptor>()
    listOf(
        ForwardDeclarationsFqNames.cNamesStructs,
        ForwardDeclarationsFqNames.objCNamesClasses,
        ForwardDeclarationsFqNames.objCNamesProtocols
    ).mapTo(result) { fqName ->
        ClassifierAliasingPackageFragmentDescriptor(
            aliasedPackageFragments,
            moduleDescriptor,
            fqName
        )
    }

    result.add(
        ExportedForwardDeclarationsPackageFragmentDescriptor(
            moduleDescriptor,
            mainPackageFqName,
            exportForwardDeclarations
        )
    )

    return result
}

/**
 * The package fragment to export forward declarations from interop package namespace, i.e.
 * redirect "$pkg.$name" to e.g. "cnames.structs.$name".
 */
class ExportedForwardDeclarationsPackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName,
    declarations: List<FqName>
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val nameToFqName = declarations.map { it.shortName() to it }.toMap()

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            val declFqName = nameToFqName[name] ?: return null

            val packageView = module.getPackage(declFqName.parent())
            return packageView.memberScope.getContributedClassifier(name, location) // FIXME(ddol): delegate to forward declarations synthetic module!
        }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("declarations = $declarations")

            p.popIndent()
            p.println("}")
        }

    }

    override fun getMemberScope() = memberScope
}

/**
 * The package fragment that redirects all requests for classifier lookup to its targets.
 */
class ClassifierAliasingPackageFragmentDescriptor(
    targets: List<KonanPackageFragmentForIde>,
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        override fun getContributedClassifier(name: Name, location: LookupLocation) =
            targets.firstNotNullResult {
                if (it.hasTopLevelClassifier(name)) {
                    it.getMemberScope().getContributedClassifier(name, location)
                } else {
                    null
                }
            }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("targets = $targets")

            p.popIndent()
            p.println("}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
}
