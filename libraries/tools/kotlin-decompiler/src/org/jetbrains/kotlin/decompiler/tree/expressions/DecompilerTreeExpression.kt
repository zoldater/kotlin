/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeStatement
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeStatementsContainer
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeVarargElement
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

interface DecompilerTreeExpression : DecompilerTreeStatement, DecompilerTreeVarargElement, SourceProducible {
    override val element: IrExpression
    val type: DecompilerTreeType
}

interface DecompilerTreeMemberAccessExpression : DecompilerTreeExpression {
    val valueArguments: List<DecompilerTreeExpression>
    val dispatchReceiver: DecompilerTreeExpression?
    val extensionReceiver: DecompilerTreeExpression?

    val valueArgumentsInsideParenthesesOrNull: String?
        get() = valueArguments.ifNotEmpty { joinToString(", ", "(", ")") { it.decompile() } }

}

class DecompilerTreeContainerExpression(
    override val element: IrContainerExpression,
    override val statements: List<DecompilerTreeStatement>,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression, DecompilerTreeStatementsContainer {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            withBraces {
                statements.forEach { println(it.decompile()) }
            }
        }
    }
}

class DecompilerTreeGetClass(
    override val element: IrGetClass,
    private val argument: DecompilerTreeExpression,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression {
    override fun produceSources(printer: SmartPrinter) {
        printer.print("${argument.decompile()}::class")
    }

}

class DecompilerTreeInstanceInitializerCall(
    override val element: IrInstanceInitializerCall,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression {
    override fun produceSources(printer: SmartPrinter) = Unit
}

class DecompilerTreeTypeOperatorCall(
    override val element: IrTypeOperatorCall,
    private val argument: DecompilerTreeExpression,
    override val type: DecompilerTreeType,
    private val typeOperand: DecompilerTreeType
) : DecompilerTreeExpression {

    override fun produceSources(printer: SmartPrinter) {
        listOfNotNull(
            argument.decompile(),
            operatorsMap[element.operator]?.let { "$it ${typeOperand.decompile()}" }
        ).joinToString(" ")
            .also { printer.print(it) }
    }

    companion object {
        val operatorsMap = mapOf(
            IrTypeOperator.CAST to "as",
            IrTypeOperator.SAFE_CAST to "as?",
            IrTypeOperator.INSTANCEOF to "is",
            IrTypeOperator.NOT_INSTANCEOF to "!is"
        )
    }
}