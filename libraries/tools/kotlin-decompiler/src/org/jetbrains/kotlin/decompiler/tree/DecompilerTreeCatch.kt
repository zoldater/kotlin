/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.insideParentheses
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeCatchParameterVariable
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeExpression
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrCatch

class DecompilerTreeCatch(
    override val element: IrCatch,
    val catchParameter: DecompilerTreeCatchParameterVariable,
    val result: DecompilerTreeExpression
) : DecompilerTreeElement, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print("catch")
            insideParentheses {
                // TODO could produce extra indent
                catchParameter.produceSources(printer)
            }
            withBraces {
                result.produceSources(printer)
            }
        }
    }
}