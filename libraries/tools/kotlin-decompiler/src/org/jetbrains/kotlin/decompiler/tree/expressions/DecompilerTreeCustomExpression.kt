/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.AbstractDecompilerTreeValueParameter
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeVariable
import org.jetbrains.kotlin.decompiler.tree.declarations.classes.AbstractDecompilerTreeClass
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrExpression

interface DecompilerTreeCustomExpression : DecompilerTreeExpression

class DecompilerTreeValueArgument(
    override val type: DecompilerTreeType,
    private val valueParameter: AbstractDecompilerTreeValueParameter,
    private val argumentValue: DecompilerTreeExpression?,
    private val toPrintWithParameterName: Boolean,
    override val element: IrExpression? = null
) : DecompilerTreeCustomExpression {
    override fun produceSources(printer: SmartPrinter) {
        argumentValue?.also {
            printer.print(
                if (toPrintWithParameterName) "${valueParameter.nameIfExists!!} = ${it.decompile()}"
                else it.decompile()
            )
        }
    }
}

class DecompilerTreeAnonymousObject(
    override val type: DecompilerTreeType,
    private val localClassDeclaration: AbstractDecompilerTreeClass,
    private val constructorCall: AbstractDecompilerTreeConstructorCall,
    override val element: IrExpression? = null
) : DecompilerTreeCustomExpression {
    override fun produceSources(printer: SmartPrinter) = localClassDeclaration.produceSources(printer)
}

class DecompilerTreeDestructingDeclaration(
    override val type: DecompilerTreeType,
    private val temporaryVariable: DecompilerTreeVariable,
    private val declarationVariables: List<DecompilerTreeVariable>,
    override val element: IrExpression? = null
) : DecompilerTreeCustomExpression {
    override fun produceSources(printer: SmartPrinter) {
        listOf(
            "val",
            declarationVariables.mapNotNull { it.nameIfExists }.joinToString(", ", "(", ")"),
            "=",
            temporaryVariable.initializer!!.decompile()
        ).joinToString(" ").also { printer.print(it) }
    }
}

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

class DecompilerTreeIncDecOperatorCall(
    override val type: DecompilerTreeType,
    private val expressionToIncrement: DecompilerTreeExpression,
    private val isPostfix: Boolean,
    private val isInc: Boolean,
    override val element: IrExpression? = null
) : DecompilerTreeCustomExpression {

    private val operatorStr = if (isInc) "++" else "--"

    override fun produceSources(printer: SmartPrinter) {
        if (isPostfix) {
            printer.print("${expressionToIncrement.asOperatorCallArgument}$operatorStr")
        } else {
            printer.print("$operatorStr${expressionToIncrement.asOperatorCallArgument}")
        }
    }
}