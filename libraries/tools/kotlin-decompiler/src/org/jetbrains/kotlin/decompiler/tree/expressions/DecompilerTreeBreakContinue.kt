/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrContinue

abstract class DecompilerTreeBreakContinue(
    override val element: IrBreakContinue,
    val token: String
) : DecompilerTreeExpression,
    SourceProducible {

    override fun produceSources(printer: SmartPrinter) {
        printer.print(token + element.label?.let { "@$it" })
    }
}

class DecompilerTreeBreak(override val element: IrBreak) : DecompilerTreeBreakContinue(element, "break")

class DecompilerTreeContinue(override val element: IrContinue) : DecompilerTreeBreakContinue(element, "continue")