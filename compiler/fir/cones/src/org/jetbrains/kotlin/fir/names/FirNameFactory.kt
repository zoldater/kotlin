/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.names

import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.fir.names.FirName

class FirNameFactory {

    private val commonNameCache: Map<String, FirName> = mutableMapOf<String, FirName>().apply {
        for (name in commonNames) {
            this[name] = create(name)
        }
    }

    private val nameCache = object : SLRUCache<String, FirName>(512, 512) {
        override fun createValue(name: String): FirName {
            return FirName.guessByFirstCharacter(name)
        }
    }

    fun create(name: String): FirName {
        return commonNameCache[name] ?: nameCache[name]
    }

    companion object {
        private val commonNames = listOf(
            "plus"
        )

        val LOCAL = FirName.special("<local>")

    }
}