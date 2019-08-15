/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.Alarm
import org.jetbrains.kotlin.idea.core.script.dependencies.FromFileAttributeScriptDependenciesLoader
import org.jetbrains.kotlin.idea.core.script.dependencies.FromRefinedConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.dependencies.OutsiderFileDependenciesLoader
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.isNonScript

class ScriptsConfigurationUpdater internal constructor(private val project: Project) {
    private val scriptsQueue = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)
    private val scriptChangesListenerDelay = 1400

    private val loaders = arrayListOf(
        FromFileAttributeScriptDependenciesLoader(),
        OutsiderFileDependenciesLoader(),
        FromRefinedConfigurationLoader(project)
    )

    fun updateDependencies(file: KtFile) {
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return

        val scriptDefinition = file.findScriptDefinition() ?: return
        loaders.filter {
            it.isApplicable(file, scriptDefinition)
        }.forEach {
            it.loadDependencies(file, scriptDefinition)
        }
    }

    init {
        listenForChangesInScripts()
    }

    private fun listenForChangesInScripts() {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                runScriptDependenciesUpdateIfNeeded(file)
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                event.newFile?.let { runScriptDependenciesUpdateIfNeeded(it) }
            }

            private fun runScriptDependenciesUpdateIfNeeded(file: VirtualFile) {
                val ktFile = getKtFileToStartConfigurationUpdate(file) ?: return

                ScriptDependenciesManager.getInstance(project).updateConfigurationsIfNotCached(listOf(ktFile))
            }
        })

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val document = event.document
                val file = FileDocumentManager.getInstance().getFile(document)?.takeIf { it.isInLocalFileSystem } ?: return
                val ktFile = getKtFileToStartConfigurationUpdate(file) ?: return

                scriptsQueue.cancelAllRequests()

                scriptsQueue.addRequest(
                    {
                        ScriptDependenciesManager.getInstance(project).updateConfigurationsIfNotCached(listOf(ktFile))
                    },
                    scriptChangesListenerDelay,
                    true
                )
            }
        }, project.messageBus.connect())
    }

    private fun getKtFileToStartConfigurationUpdate(file: VirtualFile): KtFile? {
        if (project.isDisposed || !file.isValid || file.isNonScript()) {
            return null
        }

        if (
            ApplicationManager.getApplication().isUnitTestMode &&
            ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled == true
        ) {
            return null
        }

        val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return null
        if (ProjectRootsUtil.isInProjectSource(ktFile, includeScriptsOutsideSourceRoots = true)) {
            return ktFile
        }

        return null
    }
}