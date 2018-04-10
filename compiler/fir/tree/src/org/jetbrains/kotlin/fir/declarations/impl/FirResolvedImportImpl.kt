/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirResolvedImportImpl(
    val delegate: FirImport,
    override val resolvedFqName: ClassId
) : FirResolvedImport, FirImport by delegate {
    override val packageFqName: FqName
        get() = resolvedFqName.packageFqName

    override val relativeClassName: FqName
        get() = resolvedFqName.relativeClassName

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedImport(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirResolvedImport>.acceptChildren(visitor, data)
    }

    override fun acceptChildren(visitor: FirVisitorVoid) {
        super<FirResolvedImport>.acceptChildren(visitor)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return super<FirResolvedImport>.transformChildren(transformer, data)
    }

    override fun accept(visitor: FirVisitorVoid) {
        super<FirResolvedImport>.accept(visitor)
    }

    override fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E> {
        return super<FirResolvedImport>.transform(visitor, data)
    }
}