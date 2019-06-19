/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.properties

/** Builds a [HeaderInfo] for progressions not handled by more specialized handlers. */
internal class DefaultProgressionHandler(private val context: CommonBackendContext) :
    ExpressionHandler {

    private val symbols = context.ir.symbols

    override fun match(expression: IrExpression) = ProgressionType.fromIrType(
        expression.type,
        symbols
    ) != null

    override fun build(expression: IrExpression, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // Directly use the `first/last/step` properties of the progression.
            val progression = scope.createTemporaryVariable(expression, nameHint = "progression")
            val progressionClass = progression.type.getClass()!!
            val firstProperty = progressionClass.properties.first { it.name.asString() == "first" }
            val first = irCall(firstProperty.getter!!).apply {
                dispatchReceiver = irGet(progression)
            }
            val lastProperty = progressionClass.properties.first { it.name.asString() == "last" }
            val last = irCall(lastProperty.getter!!).apply {
                dispatchReceiver = irGet(progression)
            }
            val stepProperty = progressionClass.properties.first { it.name.asString() == "step" }
            val step = irCall(stepProperty.getter!!).apply {
                dispatchReceiver = irGet(progression)
            }

            ProgressionHeaderInfo(
                ProgressionType.fromIrType(progression.type, symbols)!!,
                first,
                last,
                step,
                additionalVariables = listOf(progression),
                direction = ProgressionDirection.UNKNOWN
            )
        }
}