/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrSetField


interface DecompilerTreeFieldAccess : DecompilerTreeExpression {
    override val element: IrFieldAccessExpression
    val receiver: DecompilerTreeExpression?
}

interface AbstractDecompilerTreeGetField : DecompilerTreeFieldAccess {
    override val element: IrGetField
    override val receiver: DecompilerTreeExpression?
    override val type: DecompilerTreeType
    val lhs: String

    override fun produceSources(printer: SmartPrinter) {
        printer.print(lhs)
    }
}

class DecompilerTreeGetFieldCommon(
    override val element: IrGetField,
    override val receiver: DecompilerTreeExpression?,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeGetField {
    override val lhs: String = element.symbol.owner.name()
}

class DecompilerTreeGetFieldFromGetterSetter(
    override val element: IrGetField,
    override val receiver: DecompilerTreeExpression?,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeGetField {
    override val lhs: String = "field"
}

interface AbstractDecompilerTreeSetField : DecompilerTreeFieldAccess {
    override val element: IrSetField
    override val receiver: DecompilerTreeExpression?
    val value: DecompilerTreeExpression
    override val type: DecompilerTreeType

    val lhs: String

    override fun produceSources(printer: SmartPrinter) {
        printer.print("$lhs = ${value.decompile()}")
    }
}

class DecompilerTreeSetFieldCommon(
    override val element: IrSetField,
    override val receiver: DecompilerTreeExpression?,
    override val value: DecompilerTreeExpression,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeSetField {
    override val lhs: String = element.symbol.owner.name()
}

class DecompilerTreeSetFieldFromGetterSetter(
    override val element: IrSetField,
    override val receiver: DecompilerTreeExpression?,
    override val value: DecompilerTreeExpression,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeSetField {
    override val lhs: String = "field"
}


