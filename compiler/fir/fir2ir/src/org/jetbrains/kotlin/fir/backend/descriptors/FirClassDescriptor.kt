/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*

class FirClassDescriptor(
    symbol: FirClassSymbol, container: DeclarationDescriptor
) : FirAbstractSymbolBasedMemberDescriptor<FirClass>(symbol, container), ClassDescriptor {
    override fun getOriginal(): ClassDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnsubstitutedMemberScope(): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnsubstitutedInnerClassesScope(): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStaticScope(): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getConstructors(): Collection<ClassConstructorDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDefaultType(): SimpleType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getKind(): ClassKind = declaration.classKind

    override fun isCompanionObject(): Boolean = declaration.isCompanion

    override fun isData(): Boolean = declaration.isData

    override fun isInline(): Boolean = declaration.isInline

    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSealedSubclasses(): Collection<ClassDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTypeConstructor(): TypeConstructor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInner(): Boolean = declaration.isInner

    override fun getDeclaredTypeParameters(): List<FirTypeParameterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitClassDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }
}