/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.util.insideParentheses
import org.jetbrains.kotlin.decompiler.util.withBraces
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop

abstract class DecompilerTreeLoop(override val element: IrLoop) : DecompilerTreeExpression, SourceProducible {
    abstract val decompiledBody: DecompilerTreeExpression?
    abstract val decompiledCondition: DecompilerTreeExpression
}

class DecompilerTreeWhileLoop(
    override val element: IrWhileLoop,
    override val decompiledCondition: DecompilerTreeExpression,
    override val decompiledBody: DecompilerTreeExpression?
) : DecompilerTreeLoop(element) {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print("while ")
            insideParentheses {
                decompiledCondition.produceSources(printer)
            }
            withBraces {
                decompiledBody?.produceSources(this)
            }
        }
    }
}

class DecompilerTreeDoWhileLoop(
    override val element: IrDoWhileLoop,
    override val decompiledCondition: DecompilerTreeExpression,
    override val decompiledBody: DecompilerTreeExpression?
) : DecompilerTreeLoop(element) {
    override fun produceSources(printer: SmartPrinter) {
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