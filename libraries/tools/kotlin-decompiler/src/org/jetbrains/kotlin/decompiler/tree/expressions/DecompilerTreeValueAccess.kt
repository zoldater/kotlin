/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.util.ownerName
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression

interface DecompilerTreeValueAccess : DecompilerTreeExpression {
    override val element: IrValueAccessExpression
}

class DecompilerTreeGetValue(override val element: IrGetValue) : DecompilerTreeValueAccess {
    override fun produceSources(printer: SmartPrinter) {
        // TODO process receiver and <this> value
        printer.print(element.ownerName)
    }
}

class DecompilerTreeSetVariable(
    override val element: IrSetVariable,
    private val value: DecompilerTreeExpression
) : DecompilerTreeValueAccess, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        with(element) {
            printer.print("$ownerName ${originDescriptionsMap[origin]} ${StringBuilder()}")
        }
        value.produceSources(printer)
    }

    companion object {
        private val originDescriptionsMap = mapOf(
            IrStatementOrigin.EQ to "=",
            IrStatementOrigin.PLUSEQ to "+=",
            IrStatementOrigin.MINUSEQ to "-=",
            IrStatementOrigin.MULTEQ to "*=",
            IrStatementOrigin.DIVEQ to "/=",
            IrStatementOrigin.PERCEQ to "%="
        )
    }
}