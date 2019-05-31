/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.toFirClassLike
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName

interface FirSymbolProvider {

    fun getClassLikeSymbolByFqName(classId: FirClassId): ConeClassLikeSymbol?

    fun getSymbolByLookupTag(lookupTag: ConeClassifierLookupTag): ConeClassifierSymbol? {
        return when (lookupTag) {
            is ConeClassLikeLookupTag -> {
                (lookupTag as? ConeClassLikeLookupTagImpl)
                    ?.boundSymbol?.takeIf { it.first === this }?.let { return it.second as ConeClassifierSymbol? }

                getClassLikeSymbolByFqName(lookupTag.classId).also {
                    (lookupTag as? ConeClassLikeLookupTagImpl)
                        ?.boundSymbol = Pair(this, it)
                }
            }
            is ConeClassifierLookupTagWithFixedSymbol -> lookupTag.symbol
            else -> error("Unknown lookupTag type: ${lookupTag::class}")
        }
    }

    fun getTopLevelCallableSymbols(packageFqName: FirFqName, name: FirName): List<ConeCallableSymbol>

    fun getClassDeclaredMemberScope(classId: FirClassId): FirScope?
    fun getClassUseSiteMemberScope(
        classId: FirClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope?

    fun getAllCallableNamesInPackage(fqName: FirFqName): Set<FirName> = emptySet()
    fun getClassNamesInPackage(fqName: FirFqName): Set<FirName> = emptySet()

    fun getAllCallableNamesInClass(classId: FirClassId): Set<FirName> = emptySet()
    fun getNestedClassesNamesInClass(classId: FirClassId): Set<FirName> = emptySet()

    fun getPackage(fqName: FirFqName): FirFqName? // TODO: Replace to symbol sometime

    // TODO: should not retrieve session through the FirElement::session
    fun getSessionForClass(classId: FirClassId): FirSession? = getClassLikeSymbolByFqName(classId)?.toFirClassLike()?.session

    companion object {
        fun getInstance(session: FirSession) = session.service<FirSymbolProvider>()
    }
}

fun FirSymbolProvider.getClassDeclaredCallableSymbols(classId: FirClassId, name: FirName): List<ConeCallableSymbol> {
    val declaredMemberScope = getClassDeclaredMemberScope(classId) ?: return emptyList()
    val result = mutableListOf<ConeCallableSymbol>()
    val processor: (ConeCallableSymbol) -> ProcessorAction = {
        result.add(it)
        ProcessorAction.NEXT
    }

    declaredMemberScope.processFunctionsByName(name, processor)
    declaredMemberScope.processPropertiesByName(name, processor)

    return result
}

