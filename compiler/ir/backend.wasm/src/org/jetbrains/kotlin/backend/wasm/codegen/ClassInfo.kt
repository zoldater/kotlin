/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.ir.backend.js.utils.Signature
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol

class ClassMetadata(
    val id: Int,
    val superClass: ClassMetadata?,
    val fields: List<IrFieldSymbol>,
    val interfaces: List<InterfaceMetadata>,
    val virtualMethods: List<VirtualMethodMetadata>
) {
    val virtualMethodsSignatures: Set<Signature> =
        virtualMethods.map { it.signature }.toHashSet()
}

class InterfaceMetadata(val id: Int)

class VirtualMethodMetadata(val function: IrSimpleFunction, val signature: Signature)

fun Int.toLittleEndianBytes(): ByteArray {
    return ByteArray(4) {
        (this ushr (it * 8)).toByte()
    }
}

fun Byte.toWatData() = "\\" + this.toUByte().toString(16).padStart(2, '0')
fun ByteArray.toWatData(): String = joinToString("") { it.toWatData() }

sealed class LMElement {
    abstract val sizeInBytes: Int
    abstract fun dump(indent: String = "", startAddress: Int = 0): String
    abstract fun toBytes(): ByteArray
}

class LMFieldI32(val name: String, val value: Int) : LMElement() {
    override fun toBytes(): ByteArray = value.toLittleEndianBytes()

    override fun dump(indent: String, startAddress: Int): String {
        return "$startAddress: $indent i32   : $value      // $name\n"
    }

    override val sizeInBytes: Int = 4
}

class LMArrayI32(val name: String, val value: List<Int>) : LMElement() {
    override fun toBytes(): ByteArray {
        return value.fold(byteArrayOf()) { acc, el -> acc + el.toLittleEndianBytes() }
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "$startAddress: $indent i32[] : ${value.toIntArray().contentToString()}      // $name\n"
    }

    override val sizeInBytes: Int = value.size * 4
}

class LMStruct(val name: String, val elements: List<LMElement>) : LMElement() {
    override fun toBytes(): ByteArray {
        return elements.fold(byteArrayOf()) { acc, el -> acc + el.toBytes() }
    }

    override fun dump(indent: String, startAddress: Int): String {
        var res = "$indent// $name\n"
        var elemStartAddr = startAddress

        for (el in elements) {
            res += el.dump(indent + "  ", elemStartAddr)
            elemStartAddr += el.sizeInBytes
        }

        return res
    }

    override val sizeInBytes: Int = elements.map { it.sizeInBytes }.sum()
}