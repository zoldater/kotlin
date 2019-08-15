/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

// TODO: rename - this is not only about dependencies anymore
internal interface ScriptDependenciesLoader {
    fun isApplicable(file: KtFile, scriptDefinition: ScriptDefinition): Boolean
    fun loadDependencies(file: KtFile, scriptDefinition: ScriptDefinition)
}