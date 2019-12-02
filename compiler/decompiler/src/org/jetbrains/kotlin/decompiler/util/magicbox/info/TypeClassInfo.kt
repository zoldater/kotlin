/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox.info

import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment

/**
 * Используем для маппинга явного вывода типа, вызываемого в соответствующем скоупе, в строковое представление,
 * используемое для его отображения в генерируемом исходном коде, и информацию, необходимую для его импорта
 */
internal class TypeClassInfo(val scopeList: List<String>, val irSimpleType: IrSimpleType) : IrNodeInfo<IrSimpleType> {
    override var calculatedListForImport = irSimpleType.obtainDefaultImportStatement()
    override var calculatedName = irSimpleType.obtainDefaultName()

    companion object {
        private fun IrSimpleType.obtainDefaultImportStatement(): List<String> {
            val typeClassFqName = getClass()?.fqNameWhenAvailable?.asString()
            val typeClassPackage = getClass()?.getPackageFragment()?.fqName?.asString()
            return listOfNotNull(typeClassPackage) + (
                    typeClassFqName
                        ?.removePrefix("$typeClassPackage.")
                        ?.removeSuffix(".${this.obtainDefaultName()}")
                        ?.split(".")
                        ?.filter { it.matches("\\w+".toRegex()) }
                        ?: listOf())
        }

        //TODO Хотя тут возможно целесообразно вместо падения через toKotlinType вытаскивать, но надо смотреть
        internal fun IrSimpleType.obtainDefaultName(): String {
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
