/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.dependencies.FromRefinedConfigurationLoader
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import java.io.File
import kotlin.script.experimental.api.valueOrNull


// NOTE: this service exists exclusively because ScriptDependencyManager
// cannot be registered as implementing two services (state would be duplicated)
class IdeScriptDependenciesProvider(
    private val scriptDependenciesManager: ScriptDependenciesManager,
    project: Project
) : ScriptDependenciesProvider(project) {
    override fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? {
        return scriptDependenciesManager.getConfiguration(file)
    }
}

// TODO: rename and provide alias for compatibility - this is not only about dependencies anymore
class ScriptDependenciesManager internal constructor(private val project: Project) {
    private val cacheManager = ScriptConfigurationCacheManager(project)

    /**
     * Save configurations into cache
     * Start indexing for new class/source roots
     * Re-highlight opened scripts with changed configuration
     */
    fun saveCompilationConfigurationAfterImport(files: List<Pair<VirtualFile, ScriptCompilationConfigurationResult>>) {
        ScriptClassRootsIndexer.transaction(project) {
            for ((file, result) in files) {
                saveCompilationConfiguration(file, result, showNotification = false, skipSaveToAttributes = false)
            }
        }
    }

    /**
     * Start configuration update for files if configuration isn't up to date
     * Start indexing for new class/source roots
     *
     * @return true if update was started for any file, false if all configurations are cached
     */
    fun updateConfigurationsIfNotCached(files: List<KtFile>): Boolean {
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return false

        val notCached = files.filterNot { cacheManager.isConfigurationUpToDate(it.originalFile.virtualFile) }
        if (notCached.isNotEmpty()) {
            ScriptClassRootsIndexer.transaction(project) {
                for (file in notCached) {
                    cacheManager.updateConfiguration(file)
                }
            }
            return true
        }

        return false
    }

    /**
     * Save configuration for file into cache
     *
     * If new class/source roots are present changes the state of ScriptClassRootsIndexer to true
     * @see ScriptClassRootsIndexer.startIndexingIfNeeded should be called after this method
     *
     * Indexing process isn't started in this method as an optimization to avoid multiple calls of ScriptClassRootsIndexer
     *
     * @see ScriptDependenciesManager.getConfiguration as an example
     * @param file VirtualFile to save configuration for
     * @param configuration configuration to save
     * @param showNotification if true configuration isn't saved to cache immediately, only when user press a notification in the top of the editor
     *  @see org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings.isAutoReloadEnabled
     * @param skipSaveToAttributes if true configuration isn't saved to FileAttributes to avoid unnecessary disk write
     */
    fun saveCompilationConfiguration(
        file: VirtualFile,
        configuration: ScriptCompilationConfigurationResult,
        showNotification: Boolean,
        skipSaveToAttributes: Boolean = false
    ) {
        cacheManager.saveIfNotCached(file, configuration, showNotification, skipSaveToAttributes)
    }

    /**
     * Check if configuration is already cached for file (in cache or FileAttributes)
     * Don't check if file was changed after the last update
     * Supposed to be used to switch highlighting off for scripts without configuration
     * to avoid all file being highlighted in red
     */
    fun isConfigurationCached(file: KtFile): Boolean {
        return cacheManager.isConfigurationCached(file.originalFile.virtualFile)
    }

    /**
     * Clear configuration caches
     * Start re-highlighting for opened scripts
     */
    fun clearConfigurationCachesAndRehighlight() {
        cacheManager.clearAndRehighlight()
    }

    @Deprecated("Use getScriptClasspath(KtFile) instead")
    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> {
        val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return emptyList()
        return getScriptClasspath(ktFile)
    }

    fun getScriptClasspath(file: KtFile): List<VirtualFile> =
        toVfsRoots(getConfiguration(file)?.valueOrNull()?.dependenciesClassPath.orEmpty())

    fun getConfiguration(file: KtFile): ScriptCompilationConfigurationResult? {
        val virtualFile = file.originalFile.virtualFile

        val cached = cacheManager.getCachedConfiguration(virtualFile)
        if (cached != null) {
            return cached
        }

        if (!cacheManager.isConfigurationUpToDate(virtualFile)) {
            ScriptClassRootsIndexer.transaction(project) {
                cacheManager.updateConfiguration(file)
            }
        }

        return cacheManager.getCachedConfiguration(virtualFile)
    }

    fun getScriptDependenciesClassFilesScope(file: VirtualFile) = cacheManager.scriptDependenciesClassFilesScope(file)
    fun getScriptSdk(file: VirtualFile) = cacheManager.scriptSdk(file)

    fun getFirstScriptsSdk() = cacheManager.firstScriptSdk

    fun getAllScriptsDependenciesClassFilesScope() = cacheManager.allDependenciesClassFilesScope
    fun getAllScriptDependenciesSourcesScope() = cacheManager.allDependenciesSourcesScope

    fun getAllScriptsDependenciesClassFiles() = cacheManager.allDependenciesClassFiles
    fun getAllScriptDependenciesSources() = cacheManager.allDependenciesSources

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScriptDependenciesManager =
            ServiceManager.getService(project, ScriptDependenciesManager::class.java)

        fun getScriptDefaultSdk(project: Project): Sdk? {
            val projectSdk = ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.canBeUsedForScript() }
            if (projectSdk != null) return projectSdk

            val anyJavaSdk = getAllProjectSdks().find { it.canBeUsedForScript() }
            if (anyJavaSdk != null) {
                return anyJavaSdk
            }

            log.warn(
                "Default Script SDK is null: " +
                        "projectSdk = ${ProjectRootManager.getInstance(project).projectSdk}, " +
                        "all sdks = ${getAllProjectSdks().joinToString("\n")}"
            )
            return null
        }

        fun toVfsRoots(roots: Iterable<File>): List<VirtualFile> {
            return roots.mapNotNull { it.classpathEntryToVfs() }
        }

        private fun Sdk.canBeUsedForScript() = sdkType is JavaSdkType

        private fun File.classpathEntryToVfs(): VirtualFile? {
            val res = when {
                !exists() -> null
                isDirectory -> StandardFileSystems.local()?.findFileByPath(this.canonicalPath)
                isFile -> StandardFileSystems.jar()?.findFileByPath(this.canonicalPath + URLUtil.JAR_SEPARATOR)
                else -> null
            }
            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })
            return res
        }

        internal val log = Logger.getInstance(ScriptDependenciesManager::class.java)

        @TestOnly
        fun updateScriptDependenciesSynchronously(file: PsiFile, project: Project) {
            val loader = FromRefinedConfigurationLoader(project)
            val scriptDefinition = file.findScriptDefinition() ?: return
            assert(file is KtFile) {
                "PsiFile should be a KtFile, otherwise script dependencies cannot be loaded"
            }
            loader.runDependenciesUpdate(file as KtFile, scriptDefinition)
            ScriptClassRootsIndexer.startIndexingIfNeeded(project)
        }
    }
}
