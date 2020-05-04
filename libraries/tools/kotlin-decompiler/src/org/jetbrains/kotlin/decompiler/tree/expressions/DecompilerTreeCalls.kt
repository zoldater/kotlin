/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

abstract class AbstractDecompilerTreeCall(
    override val element: IrCall,
    override val type: DecompilerTreeType
) : DecompilerTreeMemberAccessExpression, SourceProducible {
    val leftOperand: DecompilerTreeExpression?
        get() = dispatchReceiver ?: valueArguments.getOrNull(0)

    val rightOperand: DecompilerTreeExpression?
        get() = dispatchReceiver?.let { valueArguments.getOrNull(0) } ?: valueArguments.getOrNull(1)
}

internal fun IrCall.buildCall(
    dispatchReceiver: DecompilerTreeExpression?,
    extensionReceiver: DecompilerTreeExpression?,
    valueArguments: List<DecompilerTreeExpression>,
    type: DecompilerTreeType
): AbstractDecompilerTreeCall =
    when (origin) {
        in DecompilerTreeCallUnaryOp.originMap.keys -> DecompilerTreeCallUnaryOp(
            this,
            dispatchReceiver,
            extensionReceiver,
            valueArguments,
            type
        )
        in DecompilerTreeCallBinaryOp.originMap.keys -> DecompilerTreeCallBinaryOp(
            this,
            dispatchReceiver,
            extensionReceiver,
            valueArguments, type
        )
        else -> DecompilerTreeNamedCall(this, dispatchReceiver, extensionReceiver, valueArguments, type)
    }


class DecompilerTreeCallUnaryOp(
    element: IrCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeCall(element, type) {

    override fun produceSources(printer: SmartPrinter) {
        check(element.origin in originMap.keys) { "Origin ${element.origin?.toString()} is not unary operation!" }

        leftOperand?.decompile()
            ?.also { l ->
                printer.print(
                    if (element.origin == IrStatementOrigin.EXCLEXCL)
                        "$l${originMap[element.origin]}"
                    else "${originMap[element.origin]}$l"
                )
            }
    }

    companion object {
        val originMap = mapOf(
            IrStatementOrigin.EXCLEXCL to "!!",
            IrStatementOrigin.UPLUS to "+",
            IrStatementOrigin.UMINUS to "-",
            IrStatementOrigin.EXCL to "!"
        )
    }
}

//For comparison, logical, arithmetics and assignment operators
class DecompilerTreeCallBinaryOp(
    element: IrCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeCall(element, type) {
    override fun produceSources(printer: SmartPrinter) {
        check(element.origin in originMap.keys) { "Origin ${element.origin?.toString()} is not binary operation!" }

        leftOperand?.also { l ->
            rightOperand?.also { r ->
                printer.print("${l.decompile()} ${originMap[element.origin]} ${r.decompile()}")
            }
        }
    }

    companion object {
        val originMap = mapOf(
            IrStatementOrigin.EQ to "=",
            IrStatementOrigin.PLUS to "+",
            IrStatementOrigin.MINUS to "-",
            IrStatementOrigin.MUL to "*",
            IrStatementOrigin.DIV to "/",
            IrStatementOrigin.PERC to "%",
            IrStatementOrigin.ANDAND to "&&",
            IrStatementOrigin.OROR to "||",
            IrStatementOrigin.EQEQ to "==",
            IrStatementOrigin.GT to ">",
            IrStatementOrigin.LT to "<",
            IrStatementOrigin.GTEQ to ">=",
            IrStatementOrigin.LTEQ to "<=",
            IrStatementOrigin.EXCLEQ to "!=",
            IrStatementOrigin.EQEQEQ to "===",
            IrStatementOrigin.EXCLEQEQ to "!==",
        )
    }
}

class DecompilerTreeNamedCall(
    element: IrCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeCall(element, type) {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }

}
