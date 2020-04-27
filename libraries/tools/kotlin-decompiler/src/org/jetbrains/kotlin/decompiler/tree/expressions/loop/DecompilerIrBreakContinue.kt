/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions.loop

import org.jetbrains.kotlin.decompiler.printer.DecompilerIrSourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerIrElement
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerIrExpression
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.utils.Printer

abstract class DecompilerIrBreakContinue(
    override val element: IrBreakContinue,
    val token: String
) : DecompilerIrExpression,
    DecompilerIrSourceProducible {
    override fun produceSources(printer: Printer) {
        printer.print(token + element.label?.let { "@$it" })
    }
}