/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.*

interface AbstractDecompilerTreeCall : DecompilerTreeMemberAccessExpression, SourceProducible {
    override val element: IrCall
    override val type: DecompilerTreeType
}

internal fun IrCall.buildCall(
    dispatchReceiver: DecompilerTreeExpression?,
    extensionReceiver: DecompilerTreeExpression?,
    valueArguments: List<DecompilerTreeExpression>,
    type: DecompilerTreeType,
    typeArguments: List<DecompilerTreeType>
): AbstractDecompilerTreeCall =
    when {
        origin in DecompilerTreeCallUnaryOp.originMap.keys -> DecompilerTreeCallUnaryOp(
            this,
            dispatchReceiver,
            extensionReceiver,
            valueArguments,
            type
        )
        origin in DecompilerTreeCallBinaryOp.originMap.keys -> DecompilerTreeCallBinaryOp(
            this,
            dispatchReceiver,
            extensionReceiver,
            valueArguments, type
        )
        origin == IN -> DecompilerTreeInOperatorCall(this, dispatchReceiver, extensionReceiver, valueArguments, type)
        origin == NOT_IN -> DecompilerTreeNotInOperatorCall(
            this,
            dispatchReceiver,
            extensionReceiver,
            valueArguments,
            type
        )
        origin == EQ -> DecompilerTreeCallAssignmentOp(this, dispatchReceiver, extensionReceiver, valueArguments, type)
        origin == INVOKE -> DecompilerTreeCallInvokeOp(this, dispatchReceiver, extensionReceiver, valueArguments, type)
        origin == GET_PROPERTY -> DecompilerTreeGetPropertyCall(
            this,
            dispatchReceiver,
            extensionReceiver,
            valueArguments,
            type
        )
        origin == IrStatementOrigin.RANGE -> DecompilerTreeCallRangeOp(this, dispatchReceiver, extensionReceiver, valueArguments, type)
        symbol.owner.descriptor.isInfix -> DecompilerTreeInfixFunCall(
            this,
            dispatchReceiver,
            extensionReceiver,
            valueArguments,
            type
        )
        else -> DecompilerTreeNamedCall(this, dispatchReceiver, extensionReceiver, valueArguments, type, typeArguments)
    }

class DecompilerTreeNamedCall(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>
) : AbstractDecompilerTreeCall {
    internal var separator: String = "."

    override fun produceSources(printer: SmartPrinter) {
        val callString = listOfNotNull(
            element.symbol.owner.name.asString(),
            typeArgumentsForPrint,
            valueArgumentsInsideParenthesesOrNull ?: "()"
        ).joinToString("")

        dispatchReceiver?.also {
            val lhs: String = element.superQualifierSymbol?.let { sq ->
                "super<${sq.owner.name()}>"
            } ?: it.decompile().removePrefix("<").removeSuffix(">")

            printer.print("$lhs$separator$callString")
        } ?: extensionReceiver?.decompile()?.also { printer.print("$it.$callString") }
        ?: printer.print(callString)
    }
}

class DecompilerTreeGetPropertyCall(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeCall {
    override val typeArguments: List<DecompilerTreeType>
        get() = emptyList()

    override fun produceSources(printer: SmartPrinter) {
        val callString =
            element.symbol.owner.name.asString().removePrefix("<get-").removeSuffix(">")

        dispatchReceiver?.decompile()
            ?.removePrefix("<")
            ?.removeSuffix(">")
            ?.also { printer.print("$it.$callString") }
            ?: extensionReceiver?.decompile()?.also { printer.print("$it.$callString") }
            ?: printer.print(callString)
    }
}


abstract class DecompilerTreeOperatorCall : AbstractDecompilerTreeCall {
    override val typeArguments: List<DecompilerTreeType>
        get() = emptyList()

    val leftOperand: DecompilerTreeExpression?
        get() = dispatchReceiver ?: extensionReceiver ?: valueArguments.getOrNull(0)

    val rightOperand: DecompilerTreeExpression?
        get() = dispatchReceiver?.let { valueArguments.getOrNull(0) }
            ?: extensionReceiver?.let { valueArguments.getOrNull(0) }
            ?: valueArguments.getOrNull(1)
}


class DecompilerTreeCallUnaryOp(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeOperatorCall() {
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

//For comparison, logical, arithmetics operators
class DecompilerTreeCallBinaryOp(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeOperatorCall() {
    override fun produceSources(printer: SmartPrinter) {
        check(element.origin in originMap.keys) { "Origin ${element.origin?.toString()} is not binary operation!" }

        if (element.origin in listOf(PLUSEQ, MINUSEQ, DIVEQ, MULTEQ, PERCEQ)) {
            valueArguments.firstOrNull()?.produceSources(printer)
            return
        }

        //TODO looks bad, investigate for more robust way
        if (element.origin in listOf(EXCLEQ, EXCLEQEQ) && valueArguments.isEmpty())
            leftOperand?.produceSources(printer)
        else leftOperand?.also { l ->
            rightOperand?.also { r ->
                printer.print("${l.decompile()} ${originMap[element.origin]} ${r.decompile()}")
            }
        }
    }

    companion object {
        val originMap = mapOf(
            IrStatementOrigin.PLUS to "+",
            PLUSEQ to "+",
            IrStatementOrigin.MINUS to "-",
            MINUSEQ to "-",
            IrStatementOrigin.MUL to "*",
            IrStatementOrigin.MULTEQ to "*",
            IrStatementOrigin.DIV to "/",
            DIVEQ to "/",
            IrStatementOrigin.PERC to "%",
            IrStatementOrigin.PERCEQ to "%",
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

class DecompilerTreeCallAssignmentOp(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeOperatorCall() {
    override fun produceSources(printer: SmartPrinter) {
        check(element.symbol.owner.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
            "Unexpected arguments in assignment operator!"
        }
        // TODO investigate more robust way to process `<this>` value
        val disp = leftOperand?.decompile()?.removePrefix("<")?.removeSuffix(">")?.let { "$it." } ?: ""
        // TODO investigate more robust way to obtain property name
        val lhs = element.symbol.owner.name().removePrefix("<set-").removeSuffix(">")
        printer.print("$disp$lhs = ${rightOperand?.decompile()}")
    }
}

class DecompilerTreeInOperatorCall(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeOperatorCall() {
    override fun produceSources(printer: SmartPrinter) {
        // TODO investigate more robust way to process `<this>` value
        val rhs = leftOperand!!.decompile().removePrefix("<").removeSuffix(">")
        // TODO investigate more robust way to obtain property name
        val lhs = rightOperand!!.decompile().removePrefix("<set-").removeSuffix(">")
        printer.print("$lhs in $rhs")
    }
}

class DecompilerTreeNotInOperatorCall(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeOperatorCall() {
    override fun produceSources(printer: SmartPrinter) {
        //TODO Improve this part in
        if (valueArguments.isEmpty()) {
            dispatchReceiver!!.produceSources(printer)
        } else {
            // TODO investigate more robust way to process `<this>` value
            val rhs = leftOperand!!.decompile().removePrefix("<").removeSuffix(">")
            // TODO investigate more robust way to obtain property name
            val lhs = rightOperand!!.decompile().removePrefix("<set-").removeSuffix(">")
            printer.print("$lhs !in $rhs")
        }
    }
}

class DecompilerTreeCallInvokeOp(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeOperatorCall() {

    override fun produceSources(printer: SmartPrinter) {

        dispatchReceiver?.also {
            val lhs: String = element.superQualifierSymbol?.let { sq ->
                "super<${sq.owner.name()}>"
            } ?: it.decompile().removePrefix("<").removeSuffix(">")

            printer.print("$lhs${valueArgumentsInsideParenthesesOrNull ?: "()"}")
        } ?: extensionReceiver?.decompile()?.also { printer.print("$it${valueArgumentsInsideParenthesesOrNull ?: "()"}") }
        ?: throw IllegalStateException("INVOKE operator has no dispatcher or extension receiver!")
    }
}

class DecompilerTreeCallRangeOp(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeOperatorCall() {
    override fun produceSources(printer: SmartPrinter) {
        // TODO investigate more robust way to process `<this>` value
        val lhs = dispatchReceiver!!.decompile().removePrefix("<").removeSuffix(">")
        // TODO investigate more robust way to obtain property name
        val rhs = rightOperand!!.decompile().removePrefix("<").removeSuffix(">")
        printer.print("$lhs..$rhs")
    }
}

class DecompilerTreeInfixFunCall(
    override val element: IrCall,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeOperatorCall() {
    override fun produceSources(printer: SmartPrinter) {
        // TODO investigate more robust way to process `<this>` value
        val lhs = extensionReceiver!!.decompile().removePrefix("<").removeSuffix(">")
        // TODO investigate more robust way to obtain property name
        val rhs = rightOperand!!.decompile().removePrefix("<").removeSuffix(">")
        printer.print("$lhs ${element.symbol.owner.name()} $rhs")
    }
}

