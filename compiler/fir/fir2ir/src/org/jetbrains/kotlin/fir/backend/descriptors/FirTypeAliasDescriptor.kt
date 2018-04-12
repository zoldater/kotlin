/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeSubstitutor

class FirTypeAliasDescriptor(
    symbol: FirTypeAliasSymbol, container: DeclarationDescriptor
) : FirAbstractSymbolBasedMemberDescriptor<FirTypeAlias>(symbol, container), TypeAliasDescriptor {
    override val underlyingType: SimpleType
        get() = TODO()

    override val expandedType: SimpleType = declaration.expandedType.toKotlinType()

    override val classDescriptor: ClassDescriptor?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun getOriginal(): TypeAliasDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val constructors: Collection<TypeAliasConstructorDescriptor>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun getTypeConstructor(): TypeConstructor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDefaultType(): SimpleType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInner(): Boolean = false

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitTypeAliasDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitTypeAliasDescriptor(this, null)
    }
}