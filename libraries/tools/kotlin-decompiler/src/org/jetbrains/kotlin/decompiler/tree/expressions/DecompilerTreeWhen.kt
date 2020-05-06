/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.decompile
import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.AbstractDecompilerTreeBranch
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeVariable
import org.jetbrains.kotlin.decompiler.util.OPERATOR_TOKENS
import org.jetbrains.kotlin.decompiler.util.concatenateNonEmptyWithSpace
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class AbstractDecompilerTreeWhen(
    override val element: IrWhen,
    val branches: List<AbstractDecompilerTreeBranch>,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression, SourceProducible

//TODO extend WhenWithReceiver, WhenWithDeclaringReceiver
class DecompilerTreeWhen(
    element: IrWhen,
    branches: List<AbstractDecompilerTreeBranch>,
    type: DecompilerTreeType,
) : AbstractDecompilerTreeWhen(element, branches, type) {
    val valueParameters: MutableList<DecompilerTreeVariable> = mutableListOf()

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            listOfNotNull("when",
                          valueParameters.ifNotEmpty { joinToString("; ", "(", ")") { it.decompile().trimEnd() } }
            ).joinToString("").also { print(it) }

            withBraces {
                branches.forEach { it.produceSources(this) }
            }
        }
    }
}

class DecompilerTreeIfThenElse(
    element: IrWhen,
    branches: List<AbstractDecompilerTreeBranch>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeWhen(element, branches, type) {
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
            else -> {
                branches[0].condition.decompile().let { "if ($it)" }.also { printer.print(it) }
                printer.withBraces {
                    branches[0].result.decompileByLines(printer)
                }
                branches.getOrNull(1)?.result?.also {
                    printer.print("else")
                    printer.withBraces {
                        it.decompileByLines(printer)
                    }
                }
            }
        }
    }
}