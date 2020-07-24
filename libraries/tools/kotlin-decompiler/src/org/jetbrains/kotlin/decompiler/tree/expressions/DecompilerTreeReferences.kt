/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference


class DecompilerTreeClassReference(
    override val element: IrClassReference,
    override val type: DecompilerTreeType,
    private val classType: DecompilerTreeType
) : DecompilerTreeExpression {

    override fun produceSources(printer: SmartPrinter) {
        printer.print("${classType.decompile()}::class")
    }
}

class DecompilerTreeFunctionReference(
    override val element: IrFunctionReference,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeValueArgument>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>
) : DecompilerTreeMemberAccessExpression {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreePropertyReference(
    override val element: IrPropertyReference,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeValueArgument>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>
) : DecompilerTreeMemberAccessExpression {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(element.referencedName)
    }
}

class DecompilerTreeLocalDelegatedPropertyReference(
    override val element: IrLocalDelegatedPropertyReference,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeValueArgument>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>
) : DecompilerTreeMemberAccessExpression {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}