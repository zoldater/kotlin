/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox.info

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.decompiler.util.EMPTY_TOKEN
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

/**
 * Используем для маппинга DeclarationReference, вызываемого в соответствующем скоупе, в строковое представление,
 * используемое для его отображения в генерируемом исходном коде, и информацию, необходимую для его импорта
 */
internal class TypeInfo(val scopeList: List<String>, val irType: IrType) {
    internal var calculatedListForImport = irType.obtainDefaultImportStatement()
    internal var calculatedName = irType.obtainDefaultName()

    companion object {
        private fun IrType.obtainDefaultImportStatement(): List<String> {
            val owner = this.getClass()
            //TODO понять когда owner может быть не IrDeclaration
            val ownersPackageStr = owner?.file?.fqName?.asString() ?: EMPTY_TOKEN
            val ownersFqNameStr = owner?.fqNameWhenAvailable?.asString() ?: EMPTY_TOKEN

            return ownersFqNameStr.removePrefix(ownersPackageStr).removeSuffix(".").split(".")
        }

        private fun IrType.obtainDefaultName(): String {
            //TODO Хотя тут возможно целесообразно вместо падения через toKotlinType вытаскивать, но надо смотреть
            return getClass()?.name() ?: TODO("Cannot obtain name for the type without class!")
        }

    }


}
