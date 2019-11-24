/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox.info

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.decompiler.util.EMPTY_TOKEN
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isKClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment

/**
 * Используем для маппинга DeclarationReference, вызываемого в соответствующем скоупе, в строковое представление,
 * используемое для его отображения в генерируемом исходном коде, и информацию, необходимую для его импорта
 */
internal class TypeInfo(val scopeList: List<String>, val irType: IrType) : IrNodeInfo<IrType> {
    override var calculatedListForImport = irType.obtainDefaultImportStatement()
    override var calculatedName = irType.obtainDefaultName()

    companion object {
        private fun IrType.obtainDefaultImportStatement(): List<String> {
            val typeClassFqName = getClass()?.fqNameWhenAvailable?.asString()
            val typeClassPackage = getClass()?.getPackageFragment()?.fqName?.asString()
            return listOfNotNull(typeClassPackage) + (typeClassFqName?.removePrefix("$typeClassPackage.")?.split(".")?.filter { it.matches("\\w+".toRegex()) }
                ?: listOf())
        }

        //TODO Хотя тут возможно целесообразно вместо падения через toKotlinType вытаскивать, но надо смотреть
        private fun IrType.obtainDefaultName(): String {
            val clazz = getClass()
            var defaultName = clazz?.name()
            var parent = clazz?.parent
            while (parent is IrClass) {
                defaultName = "${parent.name()}.$defaultName"
                parent = parent.parent
            }
            return defaultName ?: TODO("Cannot obtain name for the type without class!")

        }
    }


}
