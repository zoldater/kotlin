/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName

interface FirResolvedImport : FirImport {
    val packageFqName: FirFqName

    val relativeClassName: FirFqName?
    val resolvedClassId: FirClassId? get() = relativeClassName?.let { FirClassId(packageFqName, it, false) }

    val importedName: FirName? get() = importedFqName?.shortName()

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedImport(this, data)
}
