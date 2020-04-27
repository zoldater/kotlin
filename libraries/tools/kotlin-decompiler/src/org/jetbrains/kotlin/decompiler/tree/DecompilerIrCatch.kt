/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.DecompilerIrSourceProducible
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerIrVariable
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerIrExpression
import org.jetbrains.kotlin.decompiler.util.insideParentheses
import org.jetbrains.kotlin.decompiler.util.withBraces
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.utils.Printer

class DecompilerIrCatch(
    override val element: IrCatch,
    val dirCatchParameter: DecompilerIrVariable,
    val dirResult: DecompilerIrExpression
) : DecompilerIrElement<IrCatch>, DecompilerIrSourceProducible {
    override fun produceSources(printer: Printer) {
        with(printer) {
            printWithNoIndent("catch")
            insideParentheses {
                // TODO could produce extra indent
                dirCatchParameter.produceSources(printer)
            }
            withBraces {
                dirResult.produceSources(printer)
            }
        }
    }
}