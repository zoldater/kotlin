/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.builders

import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.backend.psi.endOffset
import org.jetbrains.kotlin.fir.backend.psi.startOffset
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides

class IrDelegatedPropertyBuilder(parent: IrDeclarationBuilder) : IrDeclarationBuilderExtension(parent) {
    fun generateDelegatedProperty(
        property: FirProperty,
        delegate: FirExpression,
        propertyDescriptor: PropertyDescriptor
    ): IrProperty {

        val irProperty = IrPropertyImpl(
            property.startOffset, property.endOffset, IrDeclarationOrigin.DEFINED, true,
            propertyDescriptor
        ).apply {
            // TODO: backing field
        }

        val getterDescriptor = propertyDescriptor.getter!!
        irProperty.getter = generateDelegatedPropertyAccessor(property.getter, delegate, getterDescriptor)

        if (propertyDescriptor.isVar) {
            val setterDescriptor = propertyDescriptor.setter!!
            irProperty.setter = generateDelegatedPropertyAccessor(property.setter, delegate, setterDescriptor)
        }

        return irProperty
    }

    private inline fun generateDelegatedPropertyAccessor(
        accessor: FirPropertyAccessor,
        delegate: FirExpression,
        accessorDescriptor: PropertyAccessorDescriptor
    ): IrFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            delegate.startOffset, delegate.endOffset,
            IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR,
            accessorDescriptor
        ).buildWithScope { irAccessor ->
            IrFunctionBuilder(builder).generateFunctionParameterDeclarations(
                irAccessor, accessor, null
            )
            // TODO: body
        }


}