/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.builders

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.descriptors.FirAbstractPropertyAccessorDescriptor
import org.jetbrains.kotlin.fir.backend.descriptors.FirFunctionDescriptor
import org.jetbrains.kotlin.fir.backend.descriptors.FirValueParameterDescriptor
import org.jetbrains.kotlin.fir.backend.psi.endOffset
import org.jetbrains.kotlin.fir.backend.psi.startOffset
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides

class IrFunctionBuilder(parent: IrDeclarationBuilder) : IrDeclarationBuilderExtension(parent) {
    fun generateFunctionDeclaration(function: FirNamedFunction): IrFunction =
        declareSimpleFunction(
            function,
            function.receiverType,
            IrDeclarationOrigin.DEFINED,
            FirFunctionDescriptor(function, container)
        )

    private inline fun declareSimpleFunction(
        function: FirNamedFunction,
        receiverType: FirType?,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            function.startOffset, function.endOffset, origin, descriptor
        ).buildWithScope { irFunction ->
            generateFunctionParameterDeclarations(irFunction, function, receiverType)
        }

    fun generateFunctionParameterDeclarations(
        irFunction: IrFunction,
        owner: FirElement,
        receiverType: FirType?
    ) {
        builder.generateScopedTypeParameterDeclarations(irFunction, (irFunction.descriptor as FirFunctionDescriptor).typeParameters)
        generateValueParameterDeclarations(irFunction, owner, receiverType)
    }

    fun generatePropertyAccessor(
        descriptor: FirAbstractPropertyAccessorDescriptor,
        property: FirProperty,
        accessor: FirPropertyAccessor?
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            accessor?.startOffset ?: property.startOffset,
            accessor?.endOffset ?: property.endOffset,
            if (accessor != null) IrDeclarationOrigin.DEFINED else IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR,
            descriptor
        ).buildWithScope { irAccessor ->
            builder.generateScopedTypeParameterDeclarations(irAccessor, descriptor.correspondingProperty.typeParameters)
            generateFunctionParameterDeclarations(irAccessor, accessor ?: property, property.receiverType)
            // TODO: body generation
        }


    private fun generateValueParameterDeclarations(
        irFunction: IrFunction,
        owner: FirElement,
        receiverType: FirType?
    ) {
        val functionDescriptor = irFunction.descriptor as FirFunctionDescriptor

        irFunction.dispatchReceiverParameter = functionDescriptor.dispatchReceiverParameter?.let {
            generateReceiverParameterDeclaration(it, owner)
        }

        irFunction.extensionReceiverParameter = functionDescriptor.extensionReceiverParameter?.let {
            generateReceiverParameterDeclaration(it, receiverType ?: owner)
        }

        functionDescriptor.valueParameters.mapTo(irFunction.valueParameters) { valueParameterDescriptor ->
            val parameter = valueParameterDescriptor.parameter
            generateValueParameterDeclaration(valueParameterDescriptor, parameter)
        }
    }

    private fun generateValueParameterDeclaration(
        valueParameterDescriptor: FirValueParameterDescriptor,
        parameter: FirValueParameter
    ): IrValueParameter =
        context.symbolTable.declareValueParameter(
            parameter.startOffset,
            parameter.endOffset,
            IrDeclarationOrigin.DEFINED,
            valueParameterDescriptor
        )

    private fun generateReceiverParameterDeclaration(
        receiverParameterDescriptor: ReceiverParameterDescriptor,
        receiverElement: FirElement
    ): IrValueParameter =
        context.symbolTable.declareValueParameter(
            receiverElement.startOffset,
            receiverElement.endOffset,
            IrDeclarationOrigin.DEFINED,
            receiverParameterDescriptor
        )


}