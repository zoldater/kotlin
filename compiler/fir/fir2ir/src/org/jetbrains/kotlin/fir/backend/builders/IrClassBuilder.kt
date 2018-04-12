/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.builders

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.backend.descriptors.FirClassDescriptor
import org.jetbrains.kotlin.fir.backend.psi.endOffset
import org.jetbrains.kotlin.fir.backend.psi.startOffset
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class IrClassBuilder(parent: IrDeclarationBuilder) : IrDeclarationBuilderExtension(parent) {
    fun generateClass(klass: FirClass): IrClass {
        val descriptor = FirClassDescriptor(klass.symbol, container)
        val startOffset = klass.startOffset
        val endOffset = klass.endOffset

        return context.symbolTable.declareClass(
            startOffset, endOffset, IrDeclarationOrigin.DEFINED, descriptor
        ).buildWithScope { irClass ->
            descriptor.typeConstructor.supertypes.mapNotNullTo(irClass.superClasses) {
                it.constructor.declarationDescriptor?.safeAs<ClassDescriptor>()?.let {
                    context.symbolTable.referenceClass(it)
                }
            }

            irClass.thisReceiver = context.symbolTable.declareValueParameter(
                startOffset, endOffset,
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                irClass.descriptor.thisAsReceiverParameter
            )

            builder.generateGlobalTypeParametersDeclarations(irClass, descriptor.declaredTypeParameters)

            val irPrimaryConstructor = generatePrimaryConstructor(irClass, klass)
            if (irPrimaryConstructor != null) {
                generateDeclarationsForPrimaryConstructorParameters(irClass, irPrimaryConstructor, klass)
            }

            generateMembersDeclaredInSupertypeList(irClass, klass)

            generateMembersDeclaredInClassBody(irClass, klass)

            generateFakeOverrideMemberDeclarations(irClass, klass)

            if (descriptor.isData) {
                generateAdditionalMembersForDataClass(irClass, klass)
            }

            if (descriptor.kind == ClassKind.ENUM_CLASS) {
                generateAdditionalMembersForEnumClass(irClass)
            }
        }

    }

    private fun generateAdditionalMembersForEnumClass(irClass: IrClass) {
        TODO()
    }

    private fun generateAdditionalMembersForDataClass(irClass: IrClass, klass: FirClass) {
        TODO()
    }

    private fun generateFakeOverrideMemberDeclarations(irClass: IrClass, klass: FirClass) {
        TODO()
    }

    private fun generateMembersDeclaredInClassBody(irClass: IrClass, klass: FirClass) {
        TODO()
    }

    private fun generateMembersDeclaredInSupertypeList(irClass: IrClass, klass: FirClass) {
        TODO()
    }

    private fun generateDeclarationsForPrimaryConstructorParameters(irClass: IrClass, primaryConstructor: IrConstructor, klass: FirClass) {
        TODO()
    }

    private fun generatePrimaryConstructor(irClass: IrClass, klass: FirClass): IrConstructor? {
        TODO()
    }
}