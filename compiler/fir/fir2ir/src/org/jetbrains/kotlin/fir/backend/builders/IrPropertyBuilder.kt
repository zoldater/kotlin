/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.builders

import org.jetbrains.kotlin.fir.backend.descriptors.FirPropertyDescriptor
import org.jetbrains.kotlin.fir.backend.psi.endOffset
import org.jetbrains.kotlin.fir.backend.psi.startOffset
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl

class IrPropertyBuilder(parent: IrDeclarationBuilder) : IrDeclarationBuilderExtension(parent) {

    fun generatePropertyDeclaration(property: FirProperty): IrProperty {
        val propertyDescriptor = FirPropertyDescriptor(property, container)
        val delegate = property.delegate
        return if (delegate != null) {
            generateDelegatedProperty(property, delegate, propertyDescriptor)
        } else {
            generateSimpleProperty(property, propertyDescriptor)
        }
    }

    private fun generateDelegatedProperty(
        property: FirProperty,
        delegate: FirExpression,
        propertyDescriptor: FirPropertyDescriptor
    ): IrProperty =
        IrDelegatedPropertyBuilder(builder)
            .generateDelegatedProperty(property, delegate, propertyDescriptor)


    private fun generateSimpleProperty(property: FirProperty, propertyDescriptor: FirPropertyDescriptor): IrProperty =
        IrPropertyImpl(
            property.startOffset, property.endOffset,
            IrDeclarationOrigin.DEFINED, false,
            propertyDescriptor
        ).buildWithScope { irProperty ->
            // TODO: backing field

            irProperty.getter = generateGetterIfRequired(property, propertyDescriptor)

            irProperty.setter = generateSetterIfRequired(property, propertyDescriptor)
        }

    private fun generateGetterIfRequired(property: FirProperty, descriptor: FirPropertyDescriptor): IrFunction? {
        val getter = descriptor.getter ?: return null
        return IrFunctionBuilder(builder).generatePropertyAccessor(getter, property, property.getter)
    }

    private fun generateSetterIfRequired(property: FirProperty, descriptor: FirPropertyDescriptor): IrFunction? {
        if (!property.isVar) return null
        val setter = descriptor.setter ?: return null
        return IrFunctionBuilder(builder).generatePropertyAccessor(setter, property, property.setter)
    }
}