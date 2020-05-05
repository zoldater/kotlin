/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrReturn


//TODO extend return with returnable block
interface AbstractDecompilerTreeReturn : DecompilerTreeExpression, SourceProducible {
    override val element: IrReturn
    val value: DecompilerTreeExpression
    override val type: DecompilerTreeType
}

class DecompilerTreeReturn(
    override val element: IrReturn,
    override val value: DecompilerTreeExpression,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeReturn {
    override fun produceSources(printer: SmartPrinter) {
        printer.println("return ${value.decompile()}")
    }
}

class DecompilerTreeGetterReturn(
    override val element: IrReturn,
    override val value: DecompilerTreeExpression,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeReturn {
    override fun produceSources(printer: SmartPrinter) {
        printer.println(value.decompile())
    }
}