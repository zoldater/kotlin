/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.backend.psi.toSourceElement
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractMemberDescriptor<T>(
    val declaration: T, val container: DeclarationDescriptor
) : MemberDescriptor where T : FirMemberDeclaration {
    override fun getVisibility(): Visibility = declaration.visibility

    override fun getModality(): Modality = declaration.modality!!

    override fun getName(): Name = declaration.name

    override fun isActual(): Boolean = declaration.isActual

    override fun isExpect(): Boolean = declaration.isExpect

    override fun isExternal(): Boolean = false // TODO

    override fun getSource(): SourceElement = declaration.toSourceElement()

    override fun getContainingDeclaration(): DeclarationDescriptor = container

    override val annotations: Annotations
        get() = TODO()
}

abstract class FirAbstractSymbolBasedMemberDescriptor<T>(
    symbol: FirBasedSymbol<T>, container: DeclarationDescriptor
) : FirAbstractMemberDescriptor<T>(symbol.fir, container) where T : FirMemberDeclaration, T : FirSymbolOwner<T>