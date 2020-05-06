/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.decompile
import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression

internal val IrValueAccessExpression.ownerName: String
    get() = symbol.owner.name.asString()

interface DecompilerTreeValueAccess : DecompilerTreeExpression {
    override val element: IrValueAccessExpression
}

class DecompilerTreeGetValue(override val element: IrGetValue, override val type: DecompilerTreeType) : DecompilerTreeValueAccess {
    override fun produceSources(printer: SmartPrinter) {
        // TODO process receiver and <this> value
        printer.print(element.ownerName)
    }
}

class DecompilerTreeSetVariable(
    override val element: IrSetVariable,
    private val value: DecompilerTreeExpression,
    override val type: DecompilerTreeType
) : DecompilerTreeValueAccess, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        printer.print("${element.ownerName} ${originDescriptionsMap[element.origin]} ${(value as? DecompilerTreeMemberAccessExpression)?.valueArgumentsInsideParenthesesOrNull ?: value.decompile()}")
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