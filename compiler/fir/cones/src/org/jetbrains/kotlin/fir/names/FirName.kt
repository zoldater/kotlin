/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.names

class FirName private constructor(private val name: String, val isSpecial: Boolean) : Comparable<FirName> {

    val identifier: String
        get() {
            if (isSpecial) {
                throw IllegalStateException("not identifier: $this")
            }
            return asString()
        }

    fun asString(): String = name

    override fun compareTo(other: FirName): Int = name.compareTo(other.name)

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirName) return false

        if (isSpecial != other.isSpecial) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + if (isSpecial) 1 else 0
        return result
    }

    companion object {

        @JvmStatic
        fun identifier(name: String): FirName {
            return FirName(name, false)
        }

        @JvmStatic
        fun isValidIdentifier(name: String): Boolean {
            if (name.isEmpty() || name.startsWith("<")) return false
            for (ch in name) {
                if (ch == '.' || ch == '/' || ch == '\\') {
                    return false
                }
            }

            return true
        }

        @JvmStatic
        fun special(name: String): FirName {
            if (!name.startsWith("<")) {
                throw IllegalArgumentException("special name must start with '<': $name")
            }
            return FirName(name, true)
        }

        @JvmStatic
        fun guessByFirstCharacter(name: String): FirName {
            return if (name.startsWith("<")) special(name) else identifier(name)
        }

        fun cached(name: String): FirName {
            return commonNameCache[name] ?: guessByFirstCharacter(name)
        }

        private val commonNames = listOf(
            "component1",
            "component2",
            "component3",
            "component4",
            "component5",
            "copy",
            "hasNext",
            "it",
            "iterator",
            "next",
            "plus",
            "reflect",
            "value",
            "test",
            "KClass",
            "Nothing",
            "Unit",
            "Any",
            "Enum",
            "Annotation",
            "Array",
            "Byte",
            "Short",
            "Int",
            "Long",
            "Float",
            "Double",
            "Char",
            "Boolean",
            "ByteArray",
            "ShortArray",
            "IntArray",
            "LongArray",
            "FloatArray",
            "DoubleArray",
            "CharArray",
            "BooleanArray",
            "KotlinNullPointerException",
            "<anonymous-init>",
            "<anonymous Java parameter>",
            "<array-set>",
            "<default-setter-parameter>",
            "<destruct>",
            "<error>",
            "<init>",
            "<local>",
            "<range>",
            "<unary>",
            "<unary-result>"
        )

        internal val commonNameCache: Map<String, FirName> = mutableMapOf<String, FirName>().apply {
            for (name in commonNames) {
                this[name] = guessByFirstCharacter(name)
            }
        }
    }
}
