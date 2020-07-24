/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeDeclaration
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.toKotlinType


class DecompilerTreeClassReference(
    override val element: IrClassReference,
    override val type: DecompilerTreeType,
    private val classType: DecompilerTreeType
) : DecompilerTreeExpression {

    override fun produceSources(printer: SmartPrinter) {
        printer.print("${classType.decompile()}::class")
    }
}

abstract class AbstractDecompilerTreeFunctionReference(
    override val element: IrFunctionReference,
    protected val parentDeclaration: DecompilerTreeDeclaration?,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeValueArgument>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>
) : DecompilerTreeMemberAccessExpression

class DecompilerTreeSimpleFunctionReference(
    element: IrFunctionReference,
    parentDeclaration: DecompilerTreeDeclaration?,
    dispatchReceiver: DecompilerTreeExpression?,
    extensionReceiver: DecompilerTreeExpression?,
    valueArguments: List<DecompilerTreeValueArgument>,
    type: DecompilerTreeType,
    typeArguments: List<DecompilerTreeType>
) : AbstractDecompilerTreeFunctionReference(
    element,
    parentDeclaration,
    dispatchReceiver,
    extensionReceiver,
    valueArguments,
    type,
    typeArguments
) {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun produceSources(printer: SmartPrinter) {
        val lhs = dispatchReceiver?.decompile()
            ?: extensionReceiver?.decompile()
            // TODO how to retrieve this information correctly
            ?: ((type.irType as? IrSimpleType)?.arguments?.get(0) as? IrSimpleType)
                ?.toKotlinType()?.toString()
                ?.takeIf { parentDeclaration != null || element.symbol.owner.extensionReceiverParameter != null }

        val rhs = element.referencedName.asString()
        listOfNotNull(lhs, "::", rhs)
            .joinToString("")
            .also {
                printer.print(it)
            }
    }
}

class DecompilerTreeConstructorReference(
    element: IrFunctionReference,
    parentDeclaration: DecompilerTreeDeclaration,
    dispatchReceiver: DecompilerTreeExpression?,
    extensionReceiver: DecompilerTreeExpression?,
    valueArguments: List<DecompilerTreeValueArgument>,
    type: DecompilerTreeType,
    typeArguments: List<DecompilerTreeType>
) : AbstractDecompilerTreeFunctionReference(
    element,
    parentDeclaration,
    dispatchReceiver,
    extensionReceiver,
    valueArguments,
    type,
    typeArguments
) {
    override fun produceSources(printer: SmartPrinter) {
        printer.print("::${parentDeclaration!!.nameIfExists!!}")
    }
}

class DecompilerTreePropertyReference(
    override val element: IrPropertyReference,
    protected val parentDeclaration: DecompilerTreeDeclaration?,
    override var dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeValueArgument>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>
) : DecompilerTreeMemberAccessExpression {

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun produceSources(printer: SmartPrinter) {
        val lhs = dispatchReceiver?.decompile()
            ?: extensionReceiver?.decompile()
            // TODO how to retrieve this information correctly
            ?: ((type.irType as? IrSimpleType)?.arguments?.get(0) as? IrSimpleType)
                ?.toKotlinType()?.toString()
                ?.takeIf { parentDeclaration != null || element.symbol.owner.getter?.extensionReceiverParameter != null }

        val rhs = element.referencedName.asString()
        listOfNotNull(lhs, "::", rhs)
            .joinToString("")
            .also {
                printer.print(it)
            }
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