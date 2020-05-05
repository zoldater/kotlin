/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeEnumConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeExpression
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeGetField
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeGetterReturn
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody

interface DecompilerTreeBody : DecompilerTreeElement, SourceProducible {
    override val element: IrBody
}

abstract class AbstractDecompilerTreeBlockBody(
    override val element: IrBlockBody,
    override val statements: List<DecompilerTreeStatement>
) :
    DecompilerTreeBody, DecompilerTreeStatementsContainer {
}

class DecompilerTreeBlockBody(
    element: IrBlockBody,
    statements: List<DecompilerTreeStatement>
) :
    AbstractDecompilerTreeBlockBody(element, statements) {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeGetterBody(
    element: IrBlockBody,
    statements: List<DecompilerTreeStatement>
) :
    AbstractDecompilerTreeBlockBody(element, statements) {
    val isTrivial: Boolean
        get() = (statements.getOrNull(0) as? DecompilerTreeGetterReturn)?.value as? DecompilerTreeGetField != null

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            if (statements.size == 1) {
                print(" = ${statements.first().decompile()}")
            } else {
                withBraces {
                    statements.forEach {
                        it.decompile().lines().forEach { line -> println(line) }
                    }
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
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            withBraces {
                statements.forEach {
                    it.decompile().lines().forEach { line -> println(line) }
                }
            }
        }
    }
}

abstract class AbstractDecompilerTreeExpressionBody(
    override val element: IrExpressionBody,
) : DecompilerTreeBody {
    abstract val expression: DecompilerTreeExpression
}

class DecompilerTreeExpressionBody(
    override val element: IrExpressionBody,
    override val expression: DecompilerTreeExpression
) : AbstractDecompilerTreeExpressionBody(element) {
    override fun produceSources(printer: SmartPrinter) = Unit
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
    override fun produceSources(printer: SmartPrinter) = Unit
}