/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.declarations.AbstractDecompilerTreeVariable
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

interface DecompilerTreeExpression : DecompilerTreeStatement, DecompilerTreeVarargElement, SourceProducible {
    override val element: IrExpression?
    val type: DecompilerTreeType

    val asOperatorCallArgument: String
        get() = decompile().let {
            if (this is DecompilerTreeGetValue || this is DecompilerTreeConst) {
                it
            } else {
                "($it)"
            }
        }
}

interface DecompilerTreeMemberAccessExpression : DecompilerTreeExpression, DecompilerTreeTypeArgumentsContainer {
    val valueArguments: List<DecompilerTreeValueArgument>
    var dispatchReceiver: DecompilerTreeExpression?
    val extensionReceiver: DecompilerTreeExpression?

    //TODO Print last lambda argument outer parentheses
    val valueArgumentsInsideParenthesesOrNull: String?
        get() =
            if (valueArguments.size == 1 && valueArguments[0].argumentValue is DecompilerTreeFunctionExpression)
                valueArgumentsPerComma
            else
                valueArgumentsPerComma?.let { "($it)" }

    val valueArgumentsPerComma: String?
        get() = valueArguments.map { it.decompile() }.filter { it.isNotBlank() }.ifNotEmpty { joinToString(", ") { it } }

}

interface AbstractDecompilerTreeContainerExpression : DecompilerTreeExpression, DecompilerTreeStatementsContainer {
    override val element: IrContainerExpression?
    override val statements: List<DecompilerTreeStatement>
    override val type: DecompilerTreeType

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            statements.forEach { it.decompileByLines(this) }
        }
    }
}

class DecompilerTreeContainerExpression(
    override val element: IrContainerExpression,
    override val statements: List<DecompilerTreeStatement>,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeContainerExpression

class DecompilerTreeElvisOperatorCallContainer(
    override val type: DecompilerTreeType,
    private val elvisOperatorExpression: DecompilerTreeElvisOperatorCallExpression,
    override val element: IrContainerExpression? = null,
    override val statements: List<DecompilerTreeStatement> = emptyList()
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(elvisOperatorExpression.decompile())
    }
}

class DecompilerTreeAnonymousObjectContainer(
    override val type: DecompilerTreeType,
    private val anonymousObject: DecompilerTreeAnonymousObject,
    override val element: IrContainerExpression? = null,
    override val statements: List<DecompilerTreeStatement> = emptyList()
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) = anonymousObject.produceSources(printer)
}

class DecompilerTreeDestructingDeclarationContainer(
    override val type: DecompilerTreeType,
    private val destructingDeclaration: DecompilerTreeDestructingDeclaration,
    override val element: IrContainerExpression? = null,
    override val statements: List<DecompilerTreeStatement> = emptyList()
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) = destructingDeclaration.produceSources(printer)
}

class DecompilerTreeSafeCallOperatorContainer(
    override val type: DecompilerTreeType,
    private val safeCallOperatorExpression: DecompilerTreeSafeCallOperatorExpression,
    override val element: IrContainerExpression? = null,
    override val statements: List<DecompilerTreeStatement> = emptyList()
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(safeCallOperatorExpression.decompile())
    }
}

class DecompilerTreeIncDecOperatorsContainer(
    override val type: DecompilerTreeType,
    private val incDecOperatorCall: DecompilerTreeIncDecOperatorCall,
    override val element: IrContainerExpression? = null,
    override val statements: List<DecompilerTreeStatement> = emptyList()
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) = incDecOperatorCall.produceSources(printer)
}

class DecompilerTreeWhenContainer(
    override val element: IrContainerExpression,
    override val type: DecompilerTreeType,
    val decompilerTreeWhen: AbstractDecompilerTreeWhen,
    override val statements: List<DecompilerTreeStatement> = emptyList()
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) = decompilerTreeWhen.produceSources(printer)
}


class DecompilerTreeForLoopContainer(
    override val element: IrContainerExpression,
    val iteratorVariable: AbstractDecompilerTreeVariable,
    override val statements: List<DecompilerTreeStatement>,
    override val type: DecompilerTreeType,
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print("for (${iteratorVariable.decompile()})")
            withBraces {
                statements.forEach {
                    it.decompileByLines(this)
                }
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
    private val typeOperand: DecompilerTreeType,
    private val isShorten: Boolean = false
) : DecompilerTreeExpression {

    override fun produceSources(printer: SmartPrinter) {
        listOfNotNull(argument.decompile().takeIf { !isShorten }, operatorsMap[element.operator]?.let { "$it ${typeOperand.decompile()}" })
            .joinToString(" ")
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