/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

/** Builds a [ProgressionHeaderInfo] for progressions built using the `indices` extension property. */
internal class IndicesHandler(private val context: CommonBackendContext) :
    ProgressionHandler {

    override val matcher = SimpleCalleeMatcher {
        // TODO: Handle Collection<*>.indices
        // TODO: Handle CharSequence.indices
        extensionReceiver { it != null && it.type.run { isArray() || isPrimitiveArray() } }
        fqName { it == FqName("kotlin.collections.<get-indices>") }
        parameterCount { it == 0 }
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // `last = array.size - 1` (last is inclusive) for the loop `for (i in array.indices)`.
            val arraySizeProperty = expression.extensionReceiver!!.type.getClass()!!.properties.first { it.name.asString() == "size" }
            val last = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = expression.extensionReceiver
            }.decrement()

            ProgressionHeaderInfo(
                data,
                first = irInt(0),
                last = last,
                step = irInt(1),
                canOverflow = false,
                direction = ProgressionDirection.INCREASING
            )
        }
}