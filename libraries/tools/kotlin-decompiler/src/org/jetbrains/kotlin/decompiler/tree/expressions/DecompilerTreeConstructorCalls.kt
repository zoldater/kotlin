/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall

class DecompilerTreeConstructorCall(
    override val element: IrConstructorCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeMemberAccessExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        "${type.decompile()}${valueArgumentsInsideParenthesesOrNull ?: "()"}".also { printer.print(it) }
    }
}

class DecompilerTreeDelegatingConstructorCall(
    override val element: IrDelegatingConstructorCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) :
    DecompilerTreeMemberAccessExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        "${type.decompile()}${valueArgumentsInsideParenthesesOrNull ?: "()"}".also { printer.print(it) }
    }
}

class DecompilerTreeEnumConstructorCall(
    override val element: IrEnumConstructorCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeMemberAccessExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        valueArgumentsInsideParenthesesOrNull?.also { printer.print(it) }
    }
}