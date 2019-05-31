/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName

class FirResolvedImportImpl(
    session: FirSession,
    val delegate: FirImport,
    override val packageFqName: FirFqName,
    override val relativeClassName: FirFqName?
) : FirAbstractElement(session, delegate.psi), FirResolvedImport, FirImport {
    override val aliasName: FirName?
        get() = delegate.aliasName

    override val importedFqName: FirFqName?
        get() = delegate.importedFqName

    override val isAllUnder: Boolean
        get() = delegate.isAllUnder
}
