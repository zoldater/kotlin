/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.AbstractDecompilerTreeBranch
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeVariable
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen

abstract class AbstractDecompilerTreeWhen(
    override val element: IrWhen,
    val branches: List<AbstractDecompilerTreeBranch>,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression, SourceProducible {
    abstract val subjectStr: String?

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            listOfNotNull("when", subjectStr)
                .joinToString(separator = " ")
                .also { print(it) }
            withBraces {
                branches.forEach { it.produceSources(this) }
            }
        }
    }
}

class DecompilerTreeWhenWithSubjectValue(
    element: IrWhen,
    subjectExpression: DecompilerTreeExpression,
    branches: List<AbstractDecompilerTreeBranch>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeWhen(element, branches, type) {
    override val subjectStr: String = subjectExpression.decompile().let { "($it)" }
}

class DecompilerTreeWhenWithSubjectVariable(
    element: IrWhen,
    subjectVariable: DecompilerTreeVariable,
    branches: List<AbstractDecompilerTreeBranch>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeWhen(element, branches, type) {
    override val subjectStr: String = subjectVariable.decompile().let { "($it)" }
}

class DecompilerTreeWhen(
    element: IrWhen,
    branches: List<AbstractDecompilerTreeBranch>,
    type: DecompilerTreeType,
) : AbstractDecompilerTreeWhen(element, branches, type) {
    override val subjectStr: String? = null
}

class DecompilerTreeIfThenElse(
    element: IrWhen,
    branches: List<AbstractDecompilerTreeBranch>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeWhen(element, branches, type) {
    override val subjectStr: String? = null

    private fun collectConditions(): String {
        val firstBranchCondition = branches[0].condition
        val secondBranchResult = branches[1].result
        val firstBranchDecompiled = when (firstBranchCondition) {
            is DecompilerTreeOperatorCall, is DecompilerTreeTypeOperatorCall -> firstBranchCondition.decompile()
            is DecompilerTreeIfThenElse -> firstBranchCondition.collectConditions()
            else -> throw IllegalStateException("Unexpected branch with type ${firstBranchCondition.javaClass.name}")
        }
        return listOf(firstBranchDecompiled, secondBranchResult.decompile()).filter { it.isNotBlank() }.joinToString()

    }

    override fun produceSources(printer: SmartPrinter) {
        when (element.origin) {
            IrStatementOrigin.OROR -> printer.print(
                listOfNotNull(branches[0].condition.decompile(), "||", branches[1].result.decompile())
                    .joinToString(" ")
            )
            IrStatementOrigin.ANDAND -> printer.print(
                listOfNotNull(branches[0].condition.decompile(), "&&", branches[0].result.decompile())
                    .joinToString(" ")
            )
            IrStatementOrigin.WHEN_COMMA -> printer.print(collectConditions())
            else -> {
                branches[0].condition.decompile().let { "if ($it)" }.also { printer.print(it) }
                printer.withBraces(false) {
                    branches[0].result.decompileByLines(printer)
                }
                branches.getOrNull(1)?.result?.also {
                    printer.print("else")
                    printer.withBraces(false) {
                        it.decompileByLines(printer)
                    }
                }
            }
        }
    }
}