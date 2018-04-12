/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

class FirPropertyDescriptor(
    property: FirProperty,
    container: DeclarationDescriptor
) : FirAbstractMemberDescriptor<FirProperty>(property, container), PropertyDescriptor {
    override fun getOriginal(): PropertyDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getKind(): CallableMemberDescriptor.Kind {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasSynthesizedParameterNames(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOverriddenDescriptors(): MutableCollection<out PropertyDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: Visibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): CallableMemberDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCompileTimeInitializer(): ConstantValue<*>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val getter: FirPropertyGetterDescriptor?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val setter: FirPropertySetterDescriptor?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun isSetterProjectedOut(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAccessors(): MutableList<PropertyAccessorDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTypeParameters(): List<FirTypeParameterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReturnType(): KotlinType? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasStableParameterNames(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(): KotlinType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isVar(): Boolean {
        return declaration.isVar
    }

    override fun isConst(): Boolean {
        return declaration.isConst
    }

    override fun isLateInit(): Boolean {
        return declaration.isLateInit
    }

    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out PropertyDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val isDelegated: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitPropertyDescriptor(this, null)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitPropertyDescriptor(this, null)
    }
}