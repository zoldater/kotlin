/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.util.Alarm
import com.intellij.util.containers.HashSetQueue
import org.jetbrains.kotlin.idea.core.script.ScriptClassRootsManager
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

/**
 * Sequentially loads script dependencies in background requested by [scheduleAsync].
 * Progress indicator will be shown
 *
 * States:
 *                                 silentWorker     underProgressWorker
 * - sleep
 * - silent                             x
 * - silent and under progress          x                 x
 * - under progress                                       x
 */
internal class BackgroundLoader(
    val project: Project,
    val rootsManager: ScriptClassRootsManager,
    val load: (KtFile) -> Unit
) {
    private var batchMaxSize = 0
    private val work = Any()
    private val queue: Queue<KtFile> = HashSetQueue()

    private var silentWorker: SilentWorker? = null
    private var underProgressWorker: UnderProgressWorker? = null
    private val longRunningAlarm =
        Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)
    private var longRunningAlaramRequested = false

    private var inTransaction: Boolean = false

    suspend fun schedule(file: KtFile) {

    }

    @Synchronized
    fun scheduleAsync(file: KtFile) {
        if (file in queue) return

        debug(file) { "added to update queue" }

        queue.add(file)

        batchMaxSize = maxOf(batchMaxSize, queue.size)
        updateProgress(file)

        // If the queue is longer than 3, show progress and cancel button
        if (queue.size > 3) {
            requireUnderProgressWorker()
        } else {
            requireSilentWorker()

            if (!longRunningAlaramRequested) {
                longRunningAlaramRequested = true
                longRunningAlarm.addRequest(
                    {
                        longRunningAlaramRequested = false
                        requireUnderProgressWorker()
                    },
                    1000
                )
            }
        }
    }

    @Synchronized
    private fun requireUnderProgressWorker() {
        if (queue.isEmpty() && silentWorker == null) return

        silentWorker?.stopGracefully()
        if (underProgressWorker == null) {
            underProgressWorker = UnderProgressWorker().also { it.start() }
        }
    }

    @Synchronized
    private fun requireSilentWorker() {
        if (silentWorker == null && underProgressWorker == null) {
            silentWorker = SilentWorker().also { it.start() }
        }
    }

    @Synchronized
    fun updateProgress(next: KtFile?) {
        underProgressWorker?.let {
            if (next != null) it.progressIndicator.text2 = next.virtualFilePath
            if (queue.size == 0) {
                // last file
                it.progressIndicator.isIndeterminate = true
            } else {
                it.progressIndicator.isIndeterminate = false
                val total = batchMaxSize.toDouble() + 1
                val remaining = queue.size.toDouble()
                it.progressIndicator.fraction = 1 - remaining / total + 1 / total
            }
        }
    }

    @Synchronized
    private fun ensureInTransaction() {
        if (inTransaction) return
        inTransaction = true
        rootsManager.startTransaction()
    }

    @Synchronized
    private fun endBatch() {
        check(inTransaction)
        rootsManager.commit()
        inTransaction = false
    }

    private abstract inner class Worker {
        private var shouldStop = false

        open fun start() {
            ensureInTransaction()
        }

        fun stopGracefully() {
            shouldStop = true
        }

        protected open fun checkCancelled() = false
        protected abstract fun close()

        protected fun run() {
            try {
                while (true) {
                    // prevent parallel work in both silent and under progress
                    synchronized(work) {
                        val next = synchronized(this@BackgroundLoader) {
                            if (shouldStop) return

                            if (checkCancelled() || queue.isEmpty()) {
                                endBatch()
                                return
                            }

                            queue.poll().also {
                                updateProgress(it)
                            }
                        }

                        load(next)
                    }
                }
            } finally {
                close()
            }
        }
    }

    private inner class UnderProgressWorker : Worker() {
        lateinit var progressIndicator: ProgressIndicator

        override fun start() {
            super.start()

            object : Task.Backgroundable(project, "Kotlin: Loading script dependencies...", true) {
                override fun run(indicator: ProgressIndicator) {
                    progressIndicator = indicator
                    run()
                }
            }.queue()
        }

        override fun checkCancelled(): Boolean = progressIndicator.isCanceled

        override fun close() {
            synchronized(this@BackgroundLoader) {
                underProgressWorker = null
            }
        }
    }

    private inner class SilentWorker : Worker() {
        override fun start() {
            super.start()

            BackgroundTaskUtil.executeOnPooledThread(project, Runnable {
                run()
            })
        }

        override fun close() {
            synchronized(this@BackgroundLoader) {
                silentWorker = null
            }
        }
    }
}