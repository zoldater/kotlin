/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName

class FirResolvedQualifierImpl(
    session: FirSession,
    psi: PsiElement?,
    override val packageFqName: FirFqName,
    override val relativeClassFqName: FirFqName?
) : FirResolvedQualifier, FirAbstractExpression(session, psi) {
    constructor(session: FirSession, psi: PsiElement?, classId: FirClassId) : this(
        session,
        psi,
        classId.packageFqName,
        classId.relativeClassName
    )
}