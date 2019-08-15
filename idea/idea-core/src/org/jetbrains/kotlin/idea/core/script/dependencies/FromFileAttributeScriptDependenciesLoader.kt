/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.idea.core.script.scriptCompilationConfiguration
import org.jetbrains.kotlin.idea.core.script.scriptDependencies
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper.FromCompilationConfiguration
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper.FromLegacy
import kotlin.script.experimental.api.asSuccess

// TODO: rename and provide alias for compatibility - this is not only about dependencies anymore
class FromFileAttributeScriptDependenciesLoader : ScriptDependenciesLoader {

    override fun isApplicable(file: KtFile, scriptDefinition: ScriptDefinition): Boolean {
        return file.scriptDependencies != null || file.scriptCompilationConfiguration != null
    }

    override fun loadDependencies(file: KtFile, scriptDefinition: ScriptDefinition) {
        val virtualFile = file.originalFile.virtualFile

        val configurationFromAttributes = virtualFile.scriptCompilationConfiguration?.let {
            FromCompilationConfiguration(KtFileScriptSource(file), it)
        } ?: virtualFile.scriptDependencies?.let {
            FromLegacy(KtFileScriptSource(file), it, scriptDefinition)
        }

        if (configurationFromAttributes != null) {
            debug(virtualFile) { "configuration from fileAttributes = $configurationFromAttributes" }

            if (areDependenciesValid(virtualFile, configurationFromAttributes)) {
                ScriptDependenciesManager.getInstance(file.project)
                    .saveCompilationConfiguration(
                        virtualFile,
                        configurationFromAttributes.asSuccess(),
                        showNotification = false,
                        skipSaveToAttributes = true
                    )
            } else {
                virtualFile.scriptCompilationConfiguration = null
                virtualFile.scriptDependencies = null
            }
        }
    }

    private fun areDependenciesValid(file: VirtualFile, configuration: ScriptCompilationConfigurationWrapper): Boolean {
        return configuration.dependenciesClassPath.all {
            if (it.exists()) {
                true
            } else {
                debug(file) {
                    "classpath root saved to file attribute doesn't exist: ${it.path}"
                }
                false
            }

        }
    }
}