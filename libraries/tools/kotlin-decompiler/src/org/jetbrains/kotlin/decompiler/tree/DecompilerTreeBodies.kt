/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.expressions.*
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

interface DecompilerTreeBody : DecompilerTreeElement, SourceProducible {
    override val element: IrBody
}

abstract class AbstractDecompilerTreeBlockBody(
    override val element: IrBlockBody,
    override val statements: List<DecompilerTreeStatement>
) :
    DecompilerTreeBody, DecompilerTreeStatementsContainer {
    protected fun decompileStatementsByLines(printer: SmartPrinter) {
        with(printer) {
            statements.forEach {
                it.decompile().lines().filterNot { line -> line.isEmpty() }.forEach { line -> println(line) }
            }
        }
    }
}

class DecompilerTreeBlockBody(
    element: IrBlockBody,
    statements: List<DecompilerTreeStatement>
) :
    AbstractDecompilerTreeBlockBody(element, statements) {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            when (statements.size) {
                0 -> println(" {}")
//                1 -> println(" = ${statements.first().decompile()}")
                else -> withBraces {
                    decompileStatementsByLines(this@with)
                }
            }
        }
    }
}

class DecompilerTreeGetterBody(
    element: IrBlockBody,
    statements: List<DecompilerTreeStatement>
) :
    AbstractDecompilerTreeBlockBody(element, statements) {
    val isTrivial: Boolean
        get() = (statements.getOrNull(0) as? DecompilerTreeGetterReturn)?.value as? AbstractDecompilerTreeGetField != null

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            when (statements.size) {
                0 -> println(" {}")
                1 -> println(" = ${statements.first().decompile()}")
                else -> withBraces {
                    decompileStatementsByLines(this@with)
                }
            }
        }
    }
}

class DecompilerTreeSetterBody(
    element: IrBlockBody,
    statements: List<DecompilerTreeStatement>
) :
    AbstractDecompilerTreeBlockBody(element, statements) {

    val isTrivial: Boolean
        get() = (statements.getOrNull(0) as? DecompilerTreeSetFieldFromGetterSetter)?.value as? DecompilerTreeGetValue != null

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            withBraces {
                decompileStatementsByLines(this@with)
            }
        }
    }
}


abstract class AbstractDecompilerTreeExpressionBody(
    override val element: IrExpressionBody,
) : DecompilerTreeBody {
    abstract val expression: DecompilerTreeExpression
    protected fun printExpressionByLines(printer: SmartPrinter) {
        with(printer) {
            expression.decompile()
                .lines()
                .filterNot { line -> line.isEmpty() }
                .forEach { line -> println(line) }
        }
    }
}

class DecompilerTreeExpressionBody(
    override val element: IrExpressionBody,
    override val expression: DecompilerTreeExpression
) : AbstractDecompilerTreeExpressionBody(element) {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            withBraces {
                printExpressionByLines(printer)
            }
        }
    }
}

class DecompilerTreeDefaultValueParameterInitializer(
    override val element: IrExpressionBody,
    override val expression: DecompilerTreeExpression
) : AbstractDecompilerTreeExpressionBody(element) {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print(expression.decompile().trimEnd())
        }
    }
}

class DecompilerTreeEnumEntryInitializer(
    element: IrExpressionBody,
    override val expression: DecompilerTreeEnumConstructorCall
) : AbstractDecompilerTreeExpressionBody(element) {
    override fun produceSources(printer: SmartPrinter) {
        expression.produceSources(printer)
    }
}

class DecompilerTreeFieldInitializer(
    element: IrExpressionBody,
    override val expression: DecompilerTreeExpression
) : AbstractDecompilerTreeExpressionBody(element) {
    override fun produceSources(printer: SmartPrinter) {
        expression.produceSources(printer)
    }
}

class DecompilerTreeSyntheticBody(override val element: IrSyntheticBody) : DecompilerTreeBody {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}