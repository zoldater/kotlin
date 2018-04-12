/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

class FirPropertySetterDescriptor(
    accessor: FirPropertyAccessor,
    property: FirPropertyDescriptor
) : FirAbstractPropertyAccessorDescriptor(accessor, property), PropertySetterDescriptor {
    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getKind(): CallableMemberDescriptor.Kind {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(): Name {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSource(): SourceElement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isHiddenToOvercomeSignatureClash(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasSynthesizedParameterNames(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOverriddenDescriptors(): MutableCollection<out PropertySetterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOperator(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInline(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: Visibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): PropertyAccessorDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOriginal(): PropertySetterDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isExpect(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isTailrec(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isActual(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSuspend(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReturnType(): KotlinType? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasStableParameterNames(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContainingDeclaration(): DeclarationDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInitialSignatureDescriptor(): FunctionDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInfix(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <V : Any?> getUserData(key: FunctionDescriptor.UserDataKey<V>?): V? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isDefault(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isExternal(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val correspondingVariable: VariableDescriptorWithAccessors
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val annotations: Annotations
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}