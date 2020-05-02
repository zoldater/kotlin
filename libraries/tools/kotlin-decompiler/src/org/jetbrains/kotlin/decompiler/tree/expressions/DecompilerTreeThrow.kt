/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrThrow

class DecompilerTreeThrow(
    override val element: IrThrow,
    val value: DecompilerTreeExpression
) : DecompilerTreeExpression, SourceProducible {

    override fun produceSources(printer: SmartPrinter) {
        printer.print("$THROW ${StringBuilder().also { value.produceSources(SmartPrinter(it)) }}")
    }

    companion object {
        const val THROW: String = "throw"
    }

}