/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeEnumConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeExpression
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody

interface DecompilerTreeBody : DecompilerTreeElement, SourceProducible {
    override val element: IrBody
}

class DecompilerTreeBlockBody(
    override val element: IrBlockBody,
    override val statements: List<DecompilerTreeStatement>
) :
    DecompilerTreeBody, DecompilerTreeStatementsContainer {
    override fun produceSources(printer: SmartPrinter) = Unit
}

open class DecompilerTreeExpressionBody(
    override val element: IrExpressionBody,
    open val expression: DecompilerTreeExpression
) : DecompilerTreeBody {
    override fun produceSources(printer: SmartPrinter) = Unit
}

class DecompilerTreeEnumEntryExpressionBody(
    element: IrExpressionBody,
    override val expression: DecompilerTreeEnumConstructorCall
) :
    DecompilerTreeExpressionBody(element, expression) {
    override fun produceSources(printer: SmartPrinter) {
        expression.produceSources(printer)
    }
}


class DecompilerTreeSyntheticBody(override val element: IrSyntheticBody) : DecompilerTreeBody {
    override fun produceSources(printer: SmartPrinter) = Unit
}