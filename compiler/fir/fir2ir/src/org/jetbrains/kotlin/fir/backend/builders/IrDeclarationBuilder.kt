/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.builders

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.fir.backend.FirBasedIrBuilderContext
import org.jetbrains.kotlin.fir.backend.descriptors.FirTypeAliasDescriptor
import org.jetbrains.kotlin.fir.backend.descriptors.FirTypeParameterDescriptor
import org.jetbrains.kotlin.fir.backend.psi.endOffset
import org.jetbrains.kotlin.fir.backend.psi.startOffset
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeAliasImpl

class IrDeclarationBuilder(val context: FirBasedIrBuilderContext, val container: DeclarationDescriptor) {
    fun generateMemberDeclaration(declaration: FirDeclaration): IrDeclaration {
        return when (declaration) {
            is FirNamedFunction ->
                IrFunctionBuilder(this).generateFunctionDeclaration(declaration)
            is FirProperty ->
                IrPropertyBuilder(this).generatePropertyDeclaration(declaration)
            is FirClass ->
                IrClassBuilder(this).generateClass(declaration)
            is FirTypeAlias ->
                generateTypeAliasDeclaration(declaration)
            else ->
                throw AssertionError("Unexpected member declaration: ${declaration.render()}")
        }
    }

    fun generateTypeAliasDeclaration(typeAlias: FirTypeAlias): IrDeclaration =
        IrTypeAliasImpl(
            typeAlias.startOffset, typeAlias.endOffset, IrDeclarationOrigin.DEFINED,
            FirTypeAliasDescriptor(typeAlias.symbol as FirTypeAliasSymbol, container)
        )

    fun generateGlobalTypeParametersDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<FirTypeParameterDescriptor>
    ) {
        generateTypeParameterDeclarations(irTypeParametersOwner, from) { startOffset, endOffset, typeParameterDescriptor ->
            context.symbolTable.declareGlobalTypeParameter(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                typeParameterDescriptor
            )
        }
    }

    fun generateScopedTypeParameterDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<FirTypeParameterDescriptor>
    ) {
        generateTypeParameterDeclarations(irTypeParametersOwner, from) { startOffset, endOffset, typeParameterDescriptor ->
            context.symbolTable.declareScopedTypeParameter(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                typeParameterDescriptor
            )
        }
    }

    private fun generateTypeParameterDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<FirTypeParameterDescriptor>,
        declareTypeParameter: (Int, Int, TypeParameterDescriptor) -> IrTypeParameter
    ) {
        val irTypeParameters = from.map { typeParameterDescriptor ->
            val typeParameter = typeParameterDescriptor.symbol.fir
            val startOffset = typeParameter.startOffset
            val endOffset = typeParameter.endOffset
            declareTypeParameter(
                startOffset,
                endOffset,
                typeParameterDescriptor
            )
        }

        irTypeParameters.forEach {
            mapSuperClassifiers(it.descriptor, it)
        }

        irTypeParametersOwner.typeParameters.addAll(irTypeParameters)
    }

    private fun mapSuperClassifiers(
        descriptor: TypeParameterDescriptor,
        irTypeParameter: IrTypeParameter
    ) {
        descriptor.typeConstructor.supertypes.mapNotNullTo(irTypeParameter.superClassifiers) {
            it.constructor.declarationDescriptor?.let {
                context.symbolTable.referenceClassifier(it)
            }
        }
    }
}