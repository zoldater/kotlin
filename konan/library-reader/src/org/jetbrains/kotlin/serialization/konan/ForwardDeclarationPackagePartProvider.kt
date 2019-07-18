/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer

fun createForwardDeclarationPackagePartProvider(
    storageManager: StorageManager,
    module: ModuleDescriptorImpl
): PackageFragmentProviderImpl {
    fun createPackage(fqName: FqName, supertypeName: String, classKind: ClassKind) =
        ForwardDeclarationsPackageFragmentDescriptor(
            storageManager,
            module,
            fqName,
            Name.identifier(supertypeName),
            classKind
        )

    return PackageFragmentProviderImpl(
        listOf(
            createPackage(ForwardDeclarationsFqNames.cNamesStructs, "COpaque", ClassKind.CLASS),
            createPackage(ForwardDeclarationsFqNames.objCNamesClasses, "ObjCObjectBase", ClassKind.CLASS),
            createPackage(ForwardDeclarationsFqNames.objCNamesProtocols, "ObjCObject", ClassKind.INTERFACE)
        )
    )
}

/**
 * Package fragment which creates descriptors for forward declarations on demand.
 */
private class ForwardDeclarationsPackageFragmentDescriptor(
    storageManager: StorageManager,
    module: ModuleDescriptor,
    fqName: FqName,
    supertypeName: Name,
    classKind: ClassKind
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val declarations = storageManager.createMemoizedFunction(this::createDeclaration)

        private val supertype by storageManager.createLazyValue {
            val descriptor = builtIns.builtInsModule.getPackage(ForwardDeclarationsFqNames.packageName)
                .memberScope
                .getContributedClassifier(supertypeName, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

            descriptor.defaultType
        }

        private fun createDeclaration(name: Name): ClassDescriptor {
            return ClassDescriptorImpl(
                this@ForwardDeclarationsPackageFragmentDescriptor,
                name,
                Modality.FINAL,
                classKind,
                listOf(supertype),
                SourceElement.NO_SOURCE,
                false,
                LockBasedStorageManager.NO_LOCKS
            ).apply {
                this.initialize(MemberScope.Empty, emptySet(), null)
            }
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation) = declarations(name)

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, "{}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
}