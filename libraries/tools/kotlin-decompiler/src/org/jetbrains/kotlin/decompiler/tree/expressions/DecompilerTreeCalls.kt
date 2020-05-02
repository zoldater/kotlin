/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrCall

abstract class AbstractDecompilerTreeCall(
    override val element: IrCall,
) : DecompilerTreeMemberAccessExpression, SourceProducible

class DecompilerTreeCallUnaryOp(
    element: IrCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>
) : AbstractDecompilerTreeCall(element) {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

//Both comparison operator and other binary ops
class DecompilerTreeCallBinaryOp(
    element: IrCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>
) : AbstractDecompilerTreeCall(element) {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

