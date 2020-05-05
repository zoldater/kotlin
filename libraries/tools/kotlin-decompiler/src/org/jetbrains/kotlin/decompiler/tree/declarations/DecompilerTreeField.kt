/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeExpressionBody
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrField

class DecompilerTreeField(
    override val element: IrField,
    override val annotations: List<DecompilerTreeConstructorCall>,
    val initializer: DecompilerTreeExpressionBody?,
    val type: DecompilerTreeType,
    override val annotationTarget: String? = "field"
) : DecompilerTreeDeclaration {
    override fun produceSources(printer: SmartPrinter) {
        annotationSourcesList.forEach { printer.println(it) }
        listOfNotNull(element.name().let { "$it:" }, type.decompile(), initializer?.let { "= ${it.decompile()}" })
            .joinToString(" ")
            .also { printer.println(it) }
    }
}