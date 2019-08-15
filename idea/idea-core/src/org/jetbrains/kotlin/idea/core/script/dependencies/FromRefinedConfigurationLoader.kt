/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.core.script.ScriptClassRootsIndexer
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.LegacyResolverWrapper
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver

// TODO: rename and provide alias for compatibility - this is not only about dependencies anymore
class FromRefinedConfigurationLoader internal constructor(private val project: Project) : ScriptDependenciesLoader {
    private val lock = ReentrantReadWriteLock()

    private var notifyRootChange: Boolean = false
    private var backgroundTasksQueue: LoaderBackgroundTask? = null

    override fun isApplicable(file: KtFile, scriptDefinition: ScriptDefinition) = true

    private fun isAsyncDependencyResolver(scriptDef: ScriptDefinition): Boolean =
        scriptDef.asLegacyOrNull<KotlinScriptDefinition>()?.dependencyResolver?.let {
            it is AsyncDependenciesResolver || it is LegacyResolverWrapper
        } ?: false

    override fun loadDependencies(
        file: KtFile,
        scriptDefinition: ScriptDefinition
    ) {
        if (isAsyncDependencyResolver(scriptDefinition)) {
            GlobalScope.launch {
                ScriptClassRootsIndexer.transaction(project) {
                    schedlueAsync(file)
                }
            }
        } else {
            runDependenciesUpdate(file, scriptDefinition)
        }
    }

    private suspend fun schedlueAsync(file: KtFile) {
        lock.write {
            if (backgroundTasksQueue == null) {
                backgroundTasksQueue = LoaderBackgroundTask()
                backgroundTasksQueue!!.addTask(file)
                backgroundTasksQueue!!.start()
            } else {
                backgroundTasksQueue!!.addTask(file)
            }

            suspendCoroutine<Unit> {
                backgroundTasksQueue!!.addOnFinishTask {
                    it.resume(Unit)
                }
            }
        }
    }

    private fun shouldShowNotification(): Boolean = !KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled


    // internal for tests
    internal fun runDependenciesUpdate(file: KtFile, scriptDefinition: ScriptDefinition? = file.findScriptDefinition()) {
        if (scriptDefinition == null) return

        debug(file) { "start dependencies loading" }

        val result = refineScriptCompilationConfiguration(KtFileScriptSource(file), scriptDefinition, file.project)

        debug(file) { "finish dependencies loading" }

        ScriptDependenciesManager.getInstance(file.project).saveCompilationConfiguration(
            file.originalFile.virtualFile,
            result,
            shouldShowNotification()
        )
    }

    private inner class LoaderBackgroundTask {
        private val sequenceOfFiles: ConcurrentLinkedQueue<KtFile> = ConcurrentLinkedQueue()
        private var forceStop: Boolean = false
        private var startedSilently: Boolean = false

        private var onFinish: (() -> Unit)? = null

        fun start() {
            if (shouldShowNotification()) {
                startSilently()
            } else {
                startWithProgress()
            }
        }

        private fun restartWithProgress() {
            forceStop = true
            startWithProgress()
            forceStop = false
        }

        private fun startSilently() {
            startedSilently = true
            BackgroundTaskUtil.executeOnPooledThread(project, Runnable {
                loadDependencies(EmptyProgressIndicator())
            })
        }

        private fun startWithProgress() {
            startedSilently = false
            object : Task.Backgroundable(project, "Kotlin: Loading script dependencies...", true) {
                override fun run(indicator: ProgressIndicator) {
                    loadDependencies(indicator)
                }

            }.queue()
        }

        fun addTask(file: KtFile) {
            if (sequenceOfFiles.contains(file)) return

            debug(file) { "added to update queue" }

            sequenceOfFiles.add(file)

            // If the queue is longer than 3, show progress and cancel button
            if (sequenceOfFiles.size > 3 && startedSilently) {
                restartWithProgress()
            }
        }

        fun addOnFinishTask(task: () -> Unit) {
            onFinish = task
        }

        private fun loadDependencies(indicator: ProgressIndicator?) {
            while (true) {
                if (forceStop) return
                if (indicator?.isCanceled == true || sequenceOfFiles.isEmpty()) {
                    lock.write {
                        onFinish?.invoke()
                        backgroundTasksQueue = null
                    }
                    return
                }
                runDependenciesUpdate(sequenceOfFiles.poll())
            }
        }
    }
}


