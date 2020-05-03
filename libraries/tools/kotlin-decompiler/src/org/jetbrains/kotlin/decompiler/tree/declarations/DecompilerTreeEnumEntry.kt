/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeExpressionBody
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeEnumConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry

class DecompilerTreeEnumEntry(
    override val element: IrEnumEntry,
    override val annotations: List<DecompilerTreeConstructorCall>,
    private val expressionBody: DecompilerTreeExpressionBody?
) :
    DecompilerTreeDeclaration {
    override val annotationTarget: String? = null

    override fun produceSources(printer: SmartPrinter) {

        val enumArgsConcatenated =
            (expressionBody?.expression as? DecompilerTreeEnumConstructorCall)
                ?.valueArguments?.takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "(", postfix = ")") { it.decompile() } ?: ""
        printer.print("$nameIfExists$enumArgsConcatenated")
    }
}