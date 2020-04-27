/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions.call

import org.jetbrains.kotlin.decompiler.printer.DecompilerIrSourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerIrElement
import org.jetbrains.kotlin.decompiler.tree.DecompilerIrStatement
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerIrExpression
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall

class DecompilerIrDelegatingConstructorCall(
    override val element: IrDelegatingConstructorCall,
    override val expressionParentStatement: DecompilerIrStatement
) :
    DecompilerIrExpression, DecompilerIrSourceProducible {
    override fun produceSources(): String {
        TODO("Not yet implemented")
    }
}