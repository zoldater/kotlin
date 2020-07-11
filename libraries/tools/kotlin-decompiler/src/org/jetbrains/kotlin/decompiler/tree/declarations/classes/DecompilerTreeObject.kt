/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

class DecompilerTreeObject(configuration: DecompilerTreeClassConfiguration) : AbstractDecompilerTreeClass(configuration) {
    init {
        primaryConstructor?.isObjectConstructor = true
    }

    override val keyword: String = "object"
    override val nameIfExists: String?
        get() = super.nameIfExists?.takeIf { !(element.isCompanion && it.equals("Companion", true)) }

}