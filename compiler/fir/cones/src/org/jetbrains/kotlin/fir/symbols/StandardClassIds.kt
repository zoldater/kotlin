/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName

object StandardClassIds {

    val BASE_KOTLIN_PACKAGE = FirFqName.create(FirName.identifier("kotlin"))
    private fun String.baseId() = FirClassId(BASE_KOTLIN_PACKAGE, FirName.identifier(this))
    private fun FirName.arrayId() = FirClassId(Array.packageFqName, FirName.identifier(identifier + Array.shortClassName.identifier))

    val Nothing = "Nothing".baseId()
    val Unit = "Unit".baseId()
    val Any = "Any".baseId()
    val Enum = "Enum".baseId()
    val Annotation = "Annotation".baseId()
    val Array = "Array".baseId()

    val Boolean = "Boolean".baseId()
    val Char = "Char".baseId()
    val Byte = "Byte".baseId()
    val Short = "Short".baseId()
    val Int = "Int".baseId()
    val Long = "Long".baseId()
    val Float = "Float".baseId()
    val Double = "Double".baseId()

    val String = "String".baseId()

    fun byName(name: String) = name.baseId()

    val primitiveArrayTypeByElementType: Map<FirClassId, FirClassId> = mutableMapOf<FirClassId, FirClassId>().apply {
        fun addPrimitive(id: FirClassId) {
            put(id, id.shortClassName.arrayId())
        }

        addPrimitive(Boolean)
        addPrimitive(Char)
        addPrimitive(Byte)
        addPrimitive(Short)
        addPrimitive(Int)
        addPrimitive(Long)
        addPrimitive(Float)
        addPrimitive(Double)
    }

    val elementTypeByPrimitiveArrayType = primitiveArrayTypeByElementType.map { (k, v) -> v to k }.toMap()
}