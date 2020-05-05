/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class DecompilerTreeTypeParameter(
    override val element: IrTypeParameter,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val annotationTarget: String? = null
) : DecompilerTreeDeclaration {
    private fun IrTypeParameter.variance() = variance.label
    private fun IrTypeParameter.bound() = superTypes
        .filterNot { it.isNullableAny() || it.isAny() }
        .map { decompile() }
        .ifNotEmpty { joinToString(", ", prefix = ": ") }

    override fun produceSources(printer: SmartPrinter) {
        with(element) {
            printer.print(
                listOfNotNull("reified".takeIf { isReified }, variance(), name(), bound())
                    .joinToString(" ")
            )
        }
    }
}