/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions.loop

import org.jetbrains.kotlin.decompiler.tree.DecompilerIrStatement
import org.jetbrains.kotlin.ir.expressions.IrBreak

class DecompilerIrBreak(
    override val element: IrBreak,
    override val expressionParentStatement: DecompilerIrStatement
) : DecompilerIrBreakContinue(element, "break") {
}