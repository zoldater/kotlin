/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class FirDependenciesSymbolProviderImpl(val session: FirSession) : AbstractFirSymbolProvider() {
    private val dependencyProviders by lazy {
        val moduleInfo = session.moduleInfo ?: return@lazy emptyList()
        moduleInfo.dependenciesWithoutSelf().mapNotNull {
            session.sessionProvider?.getSession(it)?.service<FirSymbolProvider>()
        }.toList()
    }

    override fun getTopLevelCallableSymbols(packageFqName: FirFqName, name: FirName): List<ConeCallableSymbol> {
        return topLevelCallableCache.lookupCacheOrCalculate(CallableId(packageFqName, null, name)) {
            dependencyProviders.flatMap { provider -> provider.getTopLevelCallableSymbols(packageFqName, name) }
        } ?: emptyList()
    }

    override fun getClassDeclaredMemberScope(classId: FirClassId) =
        dependencyProviders.firstNotNullResult { it.getClassDeclaredMemberScope(classId) }

    override fun getClassLikeSymbolByFqName(classId: FirClassId): ConeClassLikeSymbol? {
        return classCache.lookupCacheOrCalculate(classId) {
            for (provider in dependencyProviders) {
                provider.getClassLikeSymbolByFqName(classId)?.let {
                    return@lookupCacheOrCalculate it
                }
            }
            null
        }
    }

    override fun getClassUseSiteMemberScope(
        classId: FirClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        return dependencyProviders.firstNotNullResult { it.getClassUseSiteMemberScope(classId, useSiteSession, scopeSession) }
    }

    override fun getPackage(fqName: FirFqName): FirFqName? {
        return packageCache.lookupCacheOrCalculate(fqName) {
            for (provider in dependencyProviders) {
                provider.getPackage(fqName)?.let {
                    return@lookupCacheOrCalculate it
                }
            }
            null
        }
    }

    override fun getAllCallableNamesInPackage(fqName: FirFqName): Set<FirName> {
        return dependencyProviders.flatMapTo(mutableSetOf()) { it.getAllCallableNamesInPackage(fqName) }
    }

    override fun getClassNamesInPackage(fqName: FirFqName): Set<FirName> {
        return dependencyProviders.flatMapTo(mutableSetOf()) { it.getClassNamesInPackage(fqName) }
    }

    override fun getAllCallableNamesInClass(classId: FirClassId): Set<FirName> {
        return dependencyProviders.flatMapTo(mutableSetOf()) { it.getAllCallableNamesInClass(classId) }
    }

    override fun getNestedClassesNamesInClass(classId: FirClassId): Set<FirName> {
        return dependencyProviders.flatMapTo(mutableSetOf()) { it.getNestedClassesNamesInClass(classId) }
    }
}
