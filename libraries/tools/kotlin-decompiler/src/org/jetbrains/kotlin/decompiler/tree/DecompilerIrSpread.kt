/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.DecompilerIrSourceProducible
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerIrExpression
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.utils.Printer
import java.lang.StringBuilder

class DecompilerIrSpread(
    override val element: IrSpreadElement,
    private val dirExpression: DecompilerIrExpression
) : DecompilerIrElement<IrSpreadElement>, DecompilerIrSourceProducible {
    override fun produceSources(printer: Printer) {
        val sb = StringBuilder()
        sb.also {
            val childrenPrinter = Printer(it, "    ")
            dirExpression.produceSources(childrenPrinter)
        }
        printer.print("*$sb")
    }

}