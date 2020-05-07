/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeVariable
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrExpression

interface DecompilerTreeCustomExpression : DecompilerTreeExpression

class DecompilerTreeElvisOperatorCallExpression(
    override val type: DecompilerTreeType,
    private val temporaryVariable: DecompilerTreeVariable,
    private val ifThenElse: DecompilerTreeIfThenElse,
    override val element: IrExpression? = null
) : DecompilerTreeCustomExpression {
    override fun produceSources(printer: SmartPrinter) {
        temporaryVariable.initializer?.also { lhs ->
            (ifThenElse.branches[0].result as? DecompilerTreeExpression)?.also { rhs ->
                printer.print("${lhs.decompile()} ?: ${rhs.decompile()}")
            }
        }
    }
}

class DecompilerTreeSafeCallOperatorExpression(
    override val type: DecompilerTreeType,
    private val temporaryVariable: DecompilerTreeVariable,
    private val ifThenElse: DecompilerTreeIfThenElse,
    override val element: IrExpression? = null
) : DecompilerTreeCustomExpression {

    override fun produceSources(printer: SmartPrinter) {
        temporaryVariable.initializer.also { lhs ->
            (ifThenElse.branches[1].result as? DecompilerTreeNamedCall)?.apply {
                separator = "?."
                dispatchReceiver = lhs
            }?.also {
                printer.print(it.decompile())
            }
        }
    }
}
