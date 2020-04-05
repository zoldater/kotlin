/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.printer.type

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.decompiler.util.EMPTY_TOKEN
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.decompiler.util.obtain
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isLocalClass

//TODO резолвить конфликты имен типов возвращаемых значений.
// Конфликт - если более 2 записей, заканчивающихся на этот тип
internal fun IrType.obtainTypeDescription(): String =
    (this as? IrSimpleType)?.abbreviation?.let {
        with(abbreviation!!.typeAlias.owner) {
            return name() + this@obtainTypeDescription.arguments.joinToString(", ", "<", ">") { it.obtain() }
        }
    } ?: if (toKotlinType().isFunctionTypeOrSubtype) {
        with(toKotlinType()) {
            val returnType = arguments.last().type
            val inputTypes = arguments.dropLast(1)
            "${inputTypes.joinToString(", ", prefix = "(", postfix = ")") {
                it.type.toString() + ("?".takeIf { isNullable() } ?: EMPTY_TOKEN)
            }} -> $returnType"
        }
    } else getClass()!!.fqNameForIrSerialization.asString()

