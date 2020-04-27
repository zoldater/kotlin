/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.DecompilerIrSourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerIrElement
import org.jetbrains.kotlin.decompiler.tree.DecompilerIrStatement
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.utils.Printer

class DecompilerIrTry(override val element: IrTry) : DecompilerIrExpression, DecompilerIrSourceProducible {
    override fun produceSources(printer: Printer) {
        TODO("Not yet implemented")
    }
}