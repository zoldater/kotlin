/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.backend.psi.toSourceElement
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.Variance

class FirTypeParameterDescriptor(
    val symbol: FirTypeParameterSymbol,
    val container: DeclarationDescriptor
) : TypeParameterDescriptor {
    private val declaration get() = symbol.fir

    override fun getTypeConstructor(): TypeConstructor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getIndex(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCapturedFromOuterDeclaration(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(): Name {
        return declaration.name
    }

    override fun getSource(): SourceElement {
        return declaration.toSourceElement()
    }

    override fun getDefaultType(): SimpleType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContainingDeclaration(): DeclarationDescriptor {
        return container
    }

    override fun isReified(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVariance(): Variance {
        return declaration.variance
    }

    override fun getUpperBounds(): MutableList<KotlinType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOriginal(): TypeParameterDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val annotations: Annotations
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitTypeParameterDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitTypeParameterDescriptor(this, null)
    }

}