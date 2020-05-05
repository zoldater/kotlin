/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrSetField


interface DecompilerTreeFieldAccess : DecompilerTreeExpression {
    override val element: IrFieldAccessExpression
    val receiver: DecompilerTreeExpression?
    val lhs: String
        get() = "field"
}

class DecompilerTreeGetField(
    override val element: IrGetField,
    override val receiver: DecompilerTreeExpression?,
    override val type: DecompilerTreeType
) : DecompilerTreeFieldAccess {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(lhs)
    }
}

class DecompilerTreeSetField(
    override val element: IrSetField,
    override val receiver: DecompilerTreeExpression?,
    private val value: DecompilerTreeExpression,
    override val type: DecompilerTreeType
) : DecompilerTreeFieldAccess {
    override fun produceSources(printer: SmartPrinter) {
        printer.print("$lhs = ${value.decompile()}")
    }
}

