/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrVararg

class DecompilerTreeVararg(
    override val element: IrVararg,
    //TODO check the correctness with this type
    private val elements: List<SourceProducible>
) : DecompilerTreeExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        val varargElementSources = elements.joinToString(separator = ", ") { el ->
            StringBuilder().also { el.produceSources(SmartPrinter(it)) }
        }
        printer.print(varargElementSources)
    }
}