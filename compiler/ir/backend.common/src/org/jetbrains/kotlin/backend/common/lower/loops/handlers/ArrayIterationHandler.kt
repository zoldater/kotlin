/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.ArrayHeaderInfo
import org.jetbrains.kotlin.backend.common.lower.loops.ExpressionHandler
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfo
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.properties

/** Builds an [ArrayHeaderInfo] for arrays. */
internal class ArrayIterationHandler(private val context: CommonBackendContext) :
    ExpressionHandler {

    override fun match(expression: IrExpression) = expression.type.run { isArray() || isPrimitiveArray() }

    override fun build(expression: IrExpression, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // Consider the case like:
            //
            //   for (elem in A) { f(elem) }`
            //
            // If we lower it to:
            //
            //   for (i in A.indices) { f(A[i]) }
            //
            // ...then we will break program behaviour if `A` is an expression with side-effect. Instead, we lower it to:
            //
            //   val a = A
            //   for (i in a.indices) { f(a[i]) }
            //
            // This also ensures that the semantics of re-assignment of array variables used in the loop is consistent with the semantics
            // proposed in https://youtrack.jetbrains.com/issue/KT-21354.
            val arrayReference = scope.createTemporaryVariable(
                expression, nameHint = "array",
                origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
            )

            // `last = array.size` (last is exclusive) for the loop `for (i in array.indices)`.
            val arraySizeProperty = arrayReference.type.getClass()!!.properties.first { it.name.asString() == "size" }
            val last = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = irGet(arrayReference)
            }

            ArrayHeaderInfo(
                first = irInt(0),
                last = last,
                step = irInt(1),
                arrayVariable = arrayReference
            )
        }
}