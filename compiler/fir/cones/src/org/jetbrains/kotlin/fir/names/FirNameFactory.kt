/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.names

import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.fir.names.FirName.Companion.commonNameCache

class FirNameFactory {

    private val nameCache = object : SLRUCache<String, FirName>(512, 512) {
        override fun createValue(name: String): FirName {
            return FirName.guessByFirstCharacter(name)
        }
    }

    fun create(name: String): FirName {
        return commonNameCache[name] ?: nameCache[name]
    }

    companion object {
        val LOCAL = FirName.cached("<local>")

    }
}