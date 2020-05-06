/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeVariable
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

interface DecompilerTreeExpression : DecompilerTreeStatement, DecompilerTreeVarargElement, SourceProducible {
    override val element: IrExpression
    val type: DecompilerTreeType
}

interface DecompilerTreeMemberAccessExpression : DecompilerTreeExpression, DecompilerTreeTypeArgumentsContainer {
    val valueArguments: List<DecompilerTreeExpression>
    val dispatchReceiver: DecompilerTreeExpression?
    val extensionReceiver: DecompilerTreeExpression?

    val valueArgumentsInsideParenthesesOrNull: String?
        get() = valueArguments.ifNotEmpty { joinToString(", ", "(", ")") { it.decompile() } }

}

interface AbstractDecompilerTreeContainerExpression : DecompilerTreeExpression, DecompilerTreeStatementsContainer {
    override val element: IrContainerExpression
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

class DecompilerTreeWhenContainer(
    override val element: IrContainerExpression,
    override val statements: List<DecompilerTreeStatement>,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) {
//        val whenStatement = statements.last() as DecompilerTreeWhen
//        statements.filterIsInstance(DecompilerTreeVariable::class.java).also {
//            (whenStatement).valueParameters.addAll(it)
//        }
//        whenStatement.produceSources(printer)
        statements.filterIsInstance(DecompilerTreeVariable::class.java).forEach {
            printer.println(it.decompile())
        }
        statements.filterIsInstance(DecompilerTreeWhen::class.java).firstOrNull()?.also {
            printer.println(it.decompile())
        }
    }
}

class DecompilerTreeForLoopOuterContainer(
    override val element: IrContainerExpression,
    override val statements: List<DecompilerTreeStatement>,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) {
        statements.filterIsInstance(DecompilerTreeVariable::class.java).forEach {
            printer.println(it.decompile())
        }
        statements.filterIsInstance(DecompilerTreeWhileLoop::class.java).firstOrNull()?.also {
            printer.println(it.decompile())
        }
    }
}

class DecompilerTreeForLoopInnerContainer(
    override val element: IrContainerExpression,
    override val statements: List<DecompilerTreeStatement>,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeContainerExpression {
    override fun produceSources(printer: SmartPrinter) {
        statements.getOrNull(0)?.apply {
            printer.println(decompile())
        }
        (statements.getOrNull(1) as? DecompilerTreeContainerExpression)?.statements?.forEach {
            printer.println(it.decompile())
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