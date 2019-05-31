/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName

class FirSelfImportingScope(val fqName: FirFqName, val session: FirSession) : FirScope {

    private val symbolProvider = FirSymbolProvider.getInstance(session)

    private val cache = mutableMapOf<FirName, ConeClassifierSymbol?>()

    override fun processClassifiersByName(
        name: FirName,
        position: FirPosition,
        processor: (ConeClassifierSymbol) -> Boolean
    ): Boolean {
        if (name.asString().isEmpty()) return true


        val symbol = cache.getOrPut(name) {
            val unambiguousFqName = FirClassId(fqName, name)
            symbolProvider.getClassLikeSymbolByFqName(unambiguousFqName)
        }

        return if (symbol != null) {
            processor(symbol)
        } else {
            true
        }
    }
}
