/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.names.FirName

interface FirTypeParameterScope : FirScope {
    val typeParameters: Map<FirName, List<FirTypeParameter>>

    override fun processClassifiersByName(
        name: FirName,
        position: FirPosition,
        processor: (ConeClassifierSymbol) -> Boolean
    ): Boolean {
        val matchedTypeParameters = typeParameters[name] ?: return true

        return matchedTypeParameters.all { processor(it.symbol) }
    }
}
