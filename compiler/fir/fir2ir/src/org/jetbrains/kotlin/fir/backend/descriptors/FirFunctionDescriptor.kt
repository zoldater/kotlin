/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

class FirFunctionDescriptor(
    function: FirNamedFunction,
    container: DeclarationDescriptor
) : FirAbstractMemberDescriptor<FirNamedFunction>(function, container), FunctionDescriptor {
    override fun getOriginal(): FunctionDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getKind(): CallableMemberDescriptor.Kind {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isHiddenToOvercomeSignatureClash(): Boolean {
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

    override fun getReturnType(): KotlinType? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getValueParameters(): MutableList<FirValueParameterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasStableParameterNames(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasSynthesizedParameterNames(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOverriddenDescriptors(): MutableCollection<out FunctionDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInitialSignatureDescriptor(): FunctionDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: Visibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): FunctionDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOperator(): Boolean {
        return declaration.isOperator
    }

    override fun isInfix(): Boolean {
        return declaration.isInfix
    }

    override fun isInline(): Boolean {
        return declaration.isInline
    }

    override fun isTailrec(): Boolean {
        return declaration.isTailRec
    }

    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSuspend(): Boolean {
        return false // TODO()
    }

    override fun <V : Any?> getUserData(key: FunctionDescriptor.UserDataKey<V>?): V? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitFunctionDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitFunctionDescriptor(this, null)
    }


}