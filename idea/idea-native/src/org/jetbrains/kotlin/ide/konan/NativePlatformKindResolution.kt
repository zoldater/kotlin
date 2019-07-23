/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.analyzer.getCapability
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.moduleCapabilities
import org.jetbrains.kotlin.ide.konan.analyzer.NativeResolverForModuleFactory
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfosFromIdeaModel
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.KonanLibraryForIde
import org.jetbrains.kotlin.konan.library.createKonanLibraryForIde
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.konan.createForwardDeclarationPackagePartProvider
import org.jetbrains.kotlin.serialization.konan.createPackageFragmentProvider
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.konan.file.File as KFile

class NativePlatformKindResolution : IdePlatformKindResolution {

    override fun createLibraryInfo(project: Project, library: Library): List<LibraryInfo> {
        return library.getFiles(OrderRootType.CLASSES).mapNotNull { file ->
            if (!file.isKonanLibraryRoot) return@createLibraryInfo emptyList()
            val path = PathUtil.getLocalPath(file) ?: return@createLibraryInfo emptyList()
            KonanLibraryInfo(project, library, KFile(path))
        }
    }

    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean = virtualFile.isKonanLibraryRoot

    override val libraryKind: PersistentLibraryKind<*>?
        get() = NativeLibraryKind

    override val kind get() = NativeIdePlatformKind

    override val resolverForModuleFactory get() = NativeResolverForModuleFactory

    override fun createBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext) =
        createKotlinNativeBuiltIns(settings, projectContext)
}

private fun createKotlinNativeBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext): KotlinBuiltIns {
    val project = projectContext.project
    val storageManager = projectContext.storageManager

    val stdlibInfo = findNativeStdlib(project) ?: return DefaultBuiltIns.Instance
    val konanLibrary = stdlibInfo.getCapability(KonanLibraryInfo.KONAN_LIBRARY)!!

    val builtIns = KonanBuiltIns(storageManager)
    val builtInsModule = ModuleDescriptorImpl(
        KotlinBuiltIns.BUILTINS_MODULE_NAME,
        storageManager,
        builtIns,
        capabilities = stdlibInfo.capabilities
    )
    builtIns.builtInsModule = builtInsModule

    val languageSettings = IDELanguageSettingsProvider.getLanguageVersionSettings(stdlibInfo, project, settings.isReleaseCoroutines)
    val deserializationConfiguration = CompilerDeserializationConfiguration(languageSettings)

    val stdlibFragmentProvider = createPackageFragmentProvider(
        konanLibrary,
        CachingIdeKonanLibraryMetadataLoader,
        storageManager,
        builtInsModule,
        deserializationConfiguration
    )

    builtInsModule.initialize(
        CompositePackageFragmentProvider(
            listOf(
                stdlibFragmentProvider,
                createForwardDeclarationPackagePartProvider(storageManager, builtInsModule)
            )
        )
    )

    builtInsModule.setDependencies(listOf(builtInsModule))

    return builtIns
}

// TODO: It depends on a random module's stdlib, propagate the actual module here.
private fun findNativeStdlib(project: Project): KonanLibraryInfo? =
    getModuleInfosFromIdeaModel(project, KonanPlatforms.defaultKonanPlatform).firstNotNullResult { it.asNativeStdlib() }

private fun IdeaModuleInfo.asNativeStdlib(): KonanLibraryInfo? =
    if ((this as? KonanLibraryInfo)?.isStdlib == true) this else null

class KonanLibraryInfo(project: Project, library: Library, root: KFile) : LibraryInfo(project, library) {

    private val konanLibrary = createKonanLibraryForIde(root)
    private val roots = listOf(root.absolutePath)

    val isStdlib by lazy { roots.first().endsWith(KONAN_STDLIB_NAME) }

    override fun getLibraryRoots() = roots

    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() = super.capabilities +
                DeserializedKonanModuleOrigin(konanLibrary).moduleCapabilities +
                mapOf(KONAN_LIBRARY to konanLibrary)

    override val platform: TargetPlatform
        get() = KonanPlatforms.defaultKonanPlatform

    override fun toString() = "Native" + super.toString()

    companion object {
        val KONAN_LIBRARY = ModuleDescriptor.Capability<KonanLibraryForIde>("KonanLibrary")
    }
}
