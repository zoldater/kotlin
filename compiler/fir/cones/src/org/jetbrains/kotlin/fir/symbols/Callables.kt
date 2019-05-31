/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName
import org.jetbrains.kotlin.fir.names.FirNameFactory

// NB: with className == null we are at top level
data class CallableId(val packageName: FirFqName, val className: FirFqName?, val callableName: FirName) {
    val classId: FirClassId? get() = className?.let { FirClassId(packageName, it/*, false*/) }

    constructor(packageName: FirFqName, callableName: FirName) : this(packageName, null, callableName)

    @Deprecated("TODO: Better solution for local callables?")
    constructor(callableName: FirName) : this(FirFqName.create(FirNameFactory.LOCAL), null, callableName)


    override fun toString(): String {
        return buildString {
            append(packageName.toString().replace('.', '/'))
            append("/")
            if (className != null) {
                append(className)
                append(".")
            }
            append(callableName)
        }
    }

    companion object {
    }
}

interface ConeCallableSymbol : ConeSymbol {
    val callableId: CallableId
}

interface ConePropertySymbol : ConeVariableSymbol

interface ConeVariableSymbol : ConeCallableSymbol

interface ConeFunctionSymbol : ConeCallableSymbol {
    val parameters: List<ConeKotlinType>
}