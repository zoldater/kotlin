/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.names.FirName

class FirNamedReferenceWithCandidate(session: FirSession, psi: PsiElement?, name: FirName, val candidate: Candidate) :
    FirResolvedCallableReferenceImpl(session, psi, name, candidate.symbol)