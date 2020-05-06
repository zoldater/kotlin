/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.AbstractDecompilerTreeExpressionBody
import org.jetbrains.kotlin.decompiler.tree.expressions.AbstractDecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry

class DecompilerTreeEnumEntry(
    override val element: IrEnumEntry,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    private val expressionBody: AbstractDecompilerTreeExpressionBody?
) : DecompilerTreeDeclaration {
    var enumClassName: String? = null
    override val annotationTarget: String? = null

    override fun produceSources(printer: SmartPrinter) {

        val enumArgsConcatenated = expressionBody?.decompile() ?: ""
        printer.print("$nameIfExists$enumArgsConcatenated")
    }
}