/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeDeclaration
import org.jetbrains.kotlin.descriptors.Modality

class DecompilerTreeInterface(configuration: DecompilerTreeClassConfiguration) : AbstractDecompilerTreeClass(configuration) {
    override val keyword: String = "interface"

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, methods, otherPrintableDeclarations).flatten()

    override val isDefaultModality: Boolean = element.modality == Modality.ABSTRACT

    init {
        properties.forEach { it.defaultModality = Modality.ABSTRACT }
        methods.forEach { it.defaultModality = Modality.ABSTRACT }
    }
}