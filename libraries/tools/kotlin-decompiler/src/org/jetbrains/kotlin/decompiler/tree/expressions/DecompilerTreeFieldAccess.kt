/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrSetField


interface DecompilerTreeFieldAccess : DecompilerTreeExpression {
    override val element: IrFieldAccessExpression
    val receiver: DecompilerTreeExpression?

    //TODO investigate `element.symbol.owner.name.asString()` validity
    val lhs: String
        get() = receiver?.decompile()?.let { "$it.${element.symbol.owner.name.asString()}" } ?: element.symbol.owner.name.asString()
}

class DecompilerTreeGetField(override val element: IrGetField, override val receiver: DecompilerTreeExpression?) :
    DecompilerTreeFieldAccess {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(lhs)
    }
}

class DecompilerTreeSetField(
    override val element: IrSetField, override val receiver: DecompilerTreeExpression?,
    private val value: DecompilerTreeExpression
) :
    DecompilerTreeFieldAccess {
    override fun produceSources(printer: SmartPrinter) {
        printer.print("$lhs = ${value.decompile()}")
    }
}

