/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind

class DecompilerTreeConst(
    override val element: IrConst<*>,
) : DecompilerTreeExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(
            with(element) {
                when (kind) {
                    IrConstKind.String -> "\"${value.toString()}\""
                    IrConstKind.Char -> "\'${value.toString()}\'"
                    IrConstKind.Null -> "null"
                    else -> value.toString()
                }
            }
        )
    }
}
