/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import kotlin.script.experimental.api.valueOrNull

class ScriptConfigurationCacheManager internal constructor(private val project: Project) {

    private val cache = ScriptConfigurationCaches(project)
    private val cacheUpdater = ScriptsConfigurationUpdater(project)

    fun getCachedConfiguration(file: VirtualFile): ScriptCompilationConfigurationResult? = cache.getCachedConfiguration(file)

    fun isConfigurationCached(file: VirtualFile): Boolean {
        return getCachedConfiguration(file) != null || file.scriptDependencies != null || file.scriptCompilationConfiguration != null
    }

    fun isConfigurationUpToDate(file: VirtualFile): Boolean {
        return isConfigurationCached(file) && cache.isConfigurationUpToDate(file)
    }

    fun updateConfiguration(file: KtFile) {
        cacheUpdater.updateDependencies(file)
    }

    fun clearAndRehighlight() {
        updateHighlighting(cache.clearConfigurationCaches())
    }

    fun scriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope {
        return cache.scriptsDependenciesClasspathScopeCache[file] ?: GlobalSearchScope.EMPTY_SCOPE
    }

    fun scriptSdk(file: VirtualFile): Sdk? {
        return cache.scriptsSdksCache[file]
    }

    val firstScriptSdk get() = cache.firstScriptSdk

    val allDependenciesClassFilesScope get() = cache.allDependenciesClassFilesScope
    val allDependenciesSourcesScope get() = cache.allDependenciesSourcesScope
    val allDependenciesClassFiles get() = cache.allDependenciesClassFiles
    val allDependenciesSources get() = cache.allDependenciesSources

    fun saveIfNotCached(
        file: VirtualFile,
        result: ScriptCompilationConfigurationResult,
        showNotification: Boolean,
        skipFileAttributes: Boolean
    ) {
        debug(file) { "configuration received = $result" }

        val oldResult = getCachedConfiguration(file)

        if (oldResult == null) {
            saveAndRehighlightIfNeeded(file, result, skipFileAttributes)
            return
        }

        if (showNotification) {
            if (oldResult != result
                && oldResult.valueOrNull() != result.valueOrNull() // Only compilation configuration changed
                && !ApplicationManager.getApplication().isUnitTestMode
            ) {
                debug(file) {
                    "configuration changed, notification is shown: old = $oldResult, new = $result"
                }
                file.addScriptDependenciesNotificationPanel(result, project) {
                    file.removeScriptDependenciesNotificationPanel(project)
                    saveAndRehighlightIfNeeded(file, it, skipFileAttributes)
                    ScriptClassRootsIndexer.startIndexingIfNeeded(project)
                }
                return
            }

            file.removeScriptDependenciesNotificationPanel(project)
        }

        saveAndRehighlightIfNeeded(file, result, skipFileAttributes)
    }

    private fun saveAndRehighlightIfNeeded(
        file: VirtualFile,
        result: ScriptCompilationConfigurationResult,
        skipSaveToAttributes: Boolean
    ) {
        var shouldRehighlightFile = false

        if (result.reports != IdeScriptReportSink.getReports(file)) {
            debug(file) { "new script reports = ${result.reports}" }

            ServiceManager.getService(project, ScriptReportSink::class.java).attachReports(file, result.reports)
            shouldRehighlightFile = true
        }

        val oldResult = cache.getCachedConfiguration(file)
        if (result != oldResult) {
            debug(file) { "configuration changed = $result" }

            shouldRehighlightFile = true

            val configuration = result.valueOrNull()
            if (configuration != null) {
                if (cache.hasNotCachedRoots(configuration)) {
                    debug(file) { "new class roots found: $result" }

                    ScriptClassRootsIndexer.newRootsPresent = true

                    // Highlighting will be updated after indexing
//                    shouldRehighlightFile = false

                }

                if (!skipSaveToAttributes) {
                    debug(file) { "configuration saved to file attributes: $result" }

                    if (configuration is ScriptCompilationConfigurationWrapper.FromLegacy)
                        file.scriptDependencies = configuration.legacyDependencies
                    else
                        file.scriptCompilationConfiguration = configuration.configuration
                }
            }

            cache.replaceConfiguration(file, result)
            cache.clearClassRootsCaches()
        }

        if (shouldRehighlightFile) {
            updateHighlighting(listOf(file))
        }
    }

    private fun updateHighlighting(files: List<VirtualFile>) {
        if (files.isEmpty()) return

        GlobalScope.launch(EDT(project)) {
            if (project.isDisposed) return@launch

            val openFiles = FileEditorManager.getInstance(project).openFiles
            val openScripts = files.filter { it.isValid && openFiles.contains(it) }
            if (openScripts.isNotEmpty()) {
                EditorNotifications.getInstance(project).updateAllNotifications()
            }

            openScripts.forEach {
                PsiManager.getInstance(project).findFile(it)?.let { psiFile ->
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }
    }
}
