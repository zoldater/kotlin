/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.scopes.processConstructors
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.fir.symbols.ConeVariableSymbol
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirName

class FirTopLevelDeclaredMemberScope(
    file: FirFile,
    session: FirSession,
    lookupInFir: Boolean = true
) : FirAbstractProviderBasedScope(session, lookupInFir) {
    private val packageFqName = file.packageFqName

    override fun processFunctionsByName(name: FirName, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        val matchedClass = provider.getClassLikeSymbolByFqName(FirClassId(packageFqName, name))

        if (processConstructors(
                matchedClass,
                processor,
                session,
                ScopeSession(),
                name
            ).stop()
        ) {
            return STOP
        }
        val symbols = provider.getTopLevelCallableSymbols(packageFqName, name)
        for (symbol in symbols) {
            if (symbol is ConeFunctionSymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processPropertiesByName(name: FirName, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        val symbols = provider.getTopLevelCallableSymbols(packageFqName, name)
        for (symbol in symbols) {
            if (symbol is ConePropertySymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }
}
