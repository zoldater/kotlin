/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal


internal const val TYPE_INFO_VTABLE_OFFSET = 8
internal const val TYPE_INFO_VTABLE_LENGTH_OFFSET = 4
internal const val SUPER_CLASS_ID_OFFSET = 0


internal fun getVtablePtr(obj: Any): Int =
    obj.typeInfo + TYPE_INFO_VTABLE_OFFSET

internal fun getVtableLength(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + TYPE_INFO_VTABLE_LENGTH_OFFSET)

internal fun getInterfaceListLength(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + TYPE_INFO_VTABLE_LENGTH_OFFSET)

internal fun getSuperClassId(obj: Any): Int =
    wasm_i32_load(obj.typeInfo + SUPER_CLASS_ID_OFFSET)

internal fun getVirtualMethodId(obj: Any, virtualFunctionSlot: Int): Int {
    return wasm_i32_load(getVtablePtr(obj) + virtualFunctionSlot * 4)
}

internal fun getInterfaceMethodId(obj: Any, methodSignatureId: Int): Int {
    val vtableLength = getVtableLength(obj)
    val vtableSignatures = getVtablePtr(obj) + vtableLength * 4
    var virtualFunctionSlot = 0
    while (virtualFunctionSlot < vtableLength) {
        if (wasm_i32_load(vtableSignatures + virtualFunctionSlot * 4) == methodSignatureId) {
            return getVirtualMethodId(obj, virtualFunctionSlot)
        }
        virtualFunctionSlot++
    }
    wasm_unreachable()
}


internal fun isSubClassOfImpl(currentClassId: Int, otherClassId: Int): Boolean {
    if (currentClassId == otherClassId) return true
    if (currentClassId == 1 && otherClassId != 1) return false
    return isSubClassOfImpl(wasm_i32_load(currentClassId + SUPER_CLASS_ID_OFFSET), otherClassId)
}

internal fun isSubClass(obj: Any, classId: Int): Boolean {
    return isSubClassOfImpl(obj.typeInfo, classId)
}

internal fun isInterface(obj: Any, interfaceId: Int): Boolean {
    println("Is interface(obj=${obj.typeInfo}, id=$interfaceId)")
    val vtableLength = getVtableLength(obj)
    println("vtableLength: $vtableLength")

    val interfaceListSizePtr = getVtablePtr(obj) + 2 * vtableLength * 4
    println("interfaceListSizePtr: $interfaceListSizePtr")

    val interfaceListPtr = interfaceListSizePtr + 4
    println("interfaceListPtr: $interfaceListPtr")

    val ifaceListSize = wasm_i32_load(interfaceListSizePtr)
    println("ifaceListSize: $ifaceListSize")

    var ifaceSlot = 0
    while (ifaceSlot < ifaceListSize) {
        val supportedIface = wasm_i32_load(interfaceListPtr + ifaceSlot * 4)
        if (supportedIface == interfaceId)
            return true
        ifaceSlot++
    }

    return false
}

@ExcludedFromCodegen
internal fun <T> wasmClassId(): Int =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmInterfaceId(): Int =
    implementedAsIntrinsic