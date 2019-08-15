/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import org.jetbrains.kotlin.idea.util.application.runWriteAction

object ScriptClassRootsIndexer {
    var newRootsPresent = false
        set(value) {
            check(inTransaction)
            field = value
        }
    var inTransaction = false

    inline fun transaction(project: Project, body: () -> Unit) {
        inTransaction = true
        try {
            body()
        } finally {
            inTransaction = false
            startIndexingIfNeeded(project)
        }
    }

    @PublishedApi
    internal fun startIndexingIfNeeded(project: Project) {
        if (!newRootsPresent) return

        newRootsPresent = false

        val doNotifyRootsChanged = Runnable {
            runWriteAction {
                if (project.isDisposed) return@runWriteAction

                debug { "roots change event" }

                ProjectRootManagerEx.getInstanceEx(project)?.makeRootsChange(EmptyRunnable.getInstance(), false, true)
                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
            }
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            TransactionGuard.submitTransaction(project, doNotifyRootsChanged)
        } else {
            TransactionGuard.getInstance().submitTransactionLater(project, doNotifyRootsChanged)
        }
    }
}