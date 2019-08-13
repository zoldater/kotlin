/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.extensions.ExtensionPointName

interface GradleDebuggerInjector {
    fun taskType(): String
    fun backendDebuggerType(): String
    fun codeToInjectBefore(callbackPortName: String, options: String?): List<String> = emptyList()
    fun codeToInjectAfter(callbackPortName: String, options: String?): List<String> = emptyList()

    companion object {
        val EP_NAME = ExtensionPointName<GradleDebuggerInjector>("org.jetbrains.kotlin.gradleDebuggerInjector")
    }
}