/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.names

import org.jetbrains.kotlin.utils.addToStdlib.cast

class FirFqName {

    private val segments: Array<FirName>

    constructor(segments: Array<FirName>) {
        this.segments = segments
    }

    private constructor(fqName: FirFqName, name: FirName) {
        this.segments = Array<FirName>(fqName.segments.size + 1) {
            fqName.segments.getOrNull(it) ?: name
        }
    }

    fun child(name: FirName): FirFqName {
        return FirFqName(this, name)
    }

    fun parent(): FirFqName {
        return FirFqName(segments.copyOf(segments.size - 1).cast())
    }

    fun segments(): Array<out FirName> = segments

    fun isRoot(): Boolean = segments.isEmpty()

    fun shortName(): FirName = segments.lastOrNull() ?: error("root")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirFqName) return false
        if (segments.size != other.segments.size) return false
        for (index in (segments.size - 1) downTo 0) {
            if (segments[index] != other.segments[index]) return false
        }
        return true
    }

    private var hashCode = 0

    override fun hashCode(): Int {
        if (hashCode == 0 && !isRoot()) {
            hashCode = segments.foldRight(0) { segment, acc ->
                31 * acc + segment.hashCode()
            }
        }
        return hashCode
    }

    override fun toString(): String {
        return segments.joinToString(separator = ".")
    }

    companion object {

        val ROOT = create()

        fun create(vararg segments: FirName): FirFqName = FirFqName(segments as Array<FirName>)
    }
}