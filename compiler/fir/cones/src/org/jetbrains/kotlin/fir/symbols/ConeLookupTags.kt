/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirName

interface ConeClassifierLookupTag {
    val name: FirName
}

interface ConeClassifierLookupTagWithFixedSymbol {
    val symbol: ConeClassifierSymbol
}

interface ConeTypeParameterLookupTag : ConeClassifierLookupTag {
    override val name: FirName

}
interface ConeClassLikeLookupTag : ConeClassifierLookupTag {
    val classId: FirClassId

    override val name: FirName
        get() = classId.shortClassName
}

interface ConeTypeAliasLookupTag : ConeClassLikeLookupTag

interface ConeClassLookupTag : ConeClassLikeLookupTag

class ConeClassLikeLookupTagImpl(override val classId: FirClassId) : ConeClassLikeLookupTag {
    var boundSymbol: Pair<*, *>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeClassLikeLookupTagImpl

        if (classId != other.classId) return false

        return true
    }

    override fun hashCode(): Int {
        return classId.hashCode()
    }
}
