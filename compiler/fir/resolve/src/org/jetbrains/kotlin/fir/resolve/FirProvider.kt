/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName

interface FirProvider : FirSymbolProvider {
    fun getFirClassifierByFqName(fqName: FirClassId): FirMemberDeclaration?

    override fun getClassLikeSymbolByFqName(classId: FirClassId): ConeClassLikeSymbol?

    override fun getTopLevelCallableSymbols(packageFqName: FirFqName, name: FirName): List<ConeCallableSymbol>

    override fun getPackage(fqName: FirFqName): FirFqName? {
        if (getFirFilesByPackage(fqName).isNotEmpty()) return fqName
        return null
    }

    fun getFirClassifierContainerFile(fqName: FirClassId): FirFile

    fun getFirCallableContainerFile(symbol: ConeCallableSymbol): FirFile?

    companion object {
        fun getInstance(session: FirSession): FirProvider = session.service()
    }

    fun getFirFilesByPackage(fqName: FirFqName): List<FirFile>
}
