/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions.loop

import org.jetbrains.kotlin.decompiler.tree.DecompilerIrStatement
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerIrExpression
import org.jetbrains.kotlin.decompiler.util.insideParentheses
import org.jetbrains.kotlin.decompiler.util.withBraces
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.utils.Printer

class DecompilerIrDoWhileLoop(
    override val element: IrDoWhileLoop,
    override val expressionParentStatement: DecompilerIrStatement,
    override val decompiledBody: DecompilerIrExpression?,
    override val decompiledCondition: DecompilerIrExpression
) : DecompilerIrLoop(element, expressionParentStatement) {
    override fun produceSources(printer: Printer) {
        with(printer) {
            print("do")
            withBraces {
                decompiledBody?.produceSources(this)
            }
            print("while ")
            insideParentheses {
                decompiledCondition.produceSources(printer)
            }
        }
    }
}