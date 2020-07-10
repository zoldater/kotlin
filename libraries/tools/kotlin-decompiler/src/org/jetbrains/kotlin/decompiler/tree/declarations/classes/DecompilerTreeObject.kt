/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeDeclaration

class DecompilerTreeObject(configurator: DecompilerTreeClassConfigurator) : AbstractDecompilerTreeClass(configurator) {
    init {
        primaryConstructor?.isObjectConstructor = true
    }

    override val keyword: String = "object"
    override val nameIfExists: String?
        get() = if (element.isCompanion) super.nameIfExists?.takeIf { !it.equals("Companion", true) } else super.nameIfExists

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, initSections, secondaryConstructors, methods, otherPrintableDeclarations).flatten()
}