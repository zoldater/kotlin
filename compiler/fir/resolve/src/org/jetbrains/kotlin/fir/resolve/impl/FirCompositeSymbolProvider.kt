/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class FirCompositeSymbolProvider(val providers: List<FirSymbolProvider>) : FirSymbolProvider {
    override fun getClassUseSiteMemberScope(
        classId: FirClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        return providers.firstNotNullResult { it.getClassUseSiteMemberScope(classId, useSiteSession, scopeSession) }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FirFqName, name: FirName): List<ConeCallableSymbol> {
        return providers.flatMap { it.getTopLevelCallableSymbols(packageFqName, name) }
    }

    override fun getClassDeclaredMemberScope(classId: FirClassId) = providers.firstNotNullResult { it.getClassDeclaredMemberScope(classId) }

    override fun getPackage(fqName: FirFqName): FirFqName? {
        return providers.firstNotNullResult { it.getPackage(fqName) }
    }

    override fun getClassLikeSymbolByFqName(classId: FirClassId): ConeClassLikeSymbol? {
        return providers.firstNotNullResult { it.getClassLikeSymbolByFqName(classId) }
    }

    override fun getAllCallableNamesInPackage(fqName: FirFqName): Set<FirName> {
        return providers.flatMapTo(mutableSetOf()) { it.getAllCallableNamesInPackage(fqName) }
    }

    override fun getClassNamesInPackage(fqName: FirFqName): Set<FirName> {
        return providers.flatMapTo(mutableSetOf()) { it.getClassNamesInPackage(fqName) }
    }

    override fun getAllCallableNamesInClass(classId: FirClassId): Set<FirName> {
        return providers.flatMapTo(mutableSetOf()) { it.getAllCallableNamesInClass(classId) }
    }

    override fun getNestedClassesNamesInClass(classId: FirClassId): Set<FirName> {
        return providers.flatMapTo(mutableSetOf()) { it.getNestedClassesNamesInClass(classId) }
    }
}
