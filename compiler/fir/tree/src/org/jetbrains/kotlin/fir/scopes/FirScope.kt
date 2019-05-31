/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConeVariableSymbol
import org.jetbrains.kotlin.fir.names.FirName

interface FirScope {
    @Deprecated(
        "obsolete",
        replaceWith = ReplaceWith("processClassifiersByNameWithAction(name, position) { if (processor()) ProcessorAction.NEXT else ProcessorAction.STOP }.next()")
    )
    fun processClassifiersByName(
        name: FirName,
        position: FirPosition,
        processor: (ConeClassifierSymbol) -> Boolean
    ): Boolean = true

    fun processFunctionsByName(
        name: FirName,
        processor: (ConeFunctionSymbol) -> ProcessorAction
    ): ProcessorAction = NEXT

    fun processPropertiesByName(
        name: FirName,
        processor: (ConeVariableSymbol) -> ProcessorAction
    ): ProcessorAction = NEXT
}


inline fun FirScope.processClassifiersByNameWithAction(
    name: FirName,
    position: FirPosition,
    crossinline processor: (ConeClassifierSymbol) -> ProcessorAction
): ProcessorAction {
    val result = processClassifiersByName(name, position) {
        processor(it).next()
    }
    return if (result) NEXT else STOP
}

enum class FirPosition(val allowTypeParameters: Boolean = true) {
    SUPER_TYPE_OR_EXPANSION(allowTypeParameters = false),
    OTHER
}

enum class ProcessorAction {
    STOP,
    NEXT;

    operator fun not(): Boolean {
        return when (this) {
            STOP -> true
            NEXT -> false
        }
    }

    fun stop() = this == STOP
    fun next() = this == NEXT
}
