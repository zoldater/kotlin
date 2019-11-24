/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox.info

import org.jetbrains.kotlin.decompiler.util.EMPTY_TOKEN
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.util.*

/**
 * Используем для маппинга DeclarationReference, вызываемого в соответствующем скоупе, в строковое представление,
 * используемое для его отображения в генерируемом исходном коде, и информацию, необходимую для его импорта
 */
internal class DeclarationReferenceInfo(val scopeList: List<String>, val irDeclarationReference: IrDeclarationReference) :
    IrNodeInfo<IrDeclarationReference> {
    override var calculatedListForImport = irDeclarationReference.obtainDefaultImportStatement()
    override var calculatedName = irDeclarationReference.obtainDefaultName()

    companion object {
        private fun <T : IrDeclarationReference> T.obtainDefaultImportStatement(): List<String> {
            val ownerPackage = (symbol.owner as? IrDeclarationWithName)?.getPackageFragment()?.fqName?.asString()
            val ownerFqName = (symbol.owner as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString()
            return listOfNotNull(ownerPackage) +
                    (ownerFqName?.removePrefix("$ownerPackage.")?.split(".")?.filter { it.matches("\\w+".toRegex()) }
                        ?: listOf())
        }

        private fun <T : IrDeclarationReference> T.obtainDefaultName(): String {
            return when (val owner = symbol.owner) {
                !is IrDeclarationWithName -> TODO("Implemented only for node IrDeclarationWithName owner!")
                is IrConstructor -> owner.parentAsClass.name()
                is IrEnumEntry, is IrTypeAlias -> owner.name.asString()
                is IrClass -> {
                    var defaultName = owner.name.asString()
                    var parent = owner.parent
                    while (parent is IrClass) {
                        defaultName = "${parent.name()}.$defaultName"
                        parent = parent.parent
                    }
                    return defaultName
                }
                //Если функция или property - топ-левел или объявлена внутри Companion object,
                // то ее можно импортировать и обращатсья по имени.
                is IrSimpleFunction, is IrProperty -> {
                    if (owner.parent is IrFile || owner.parentAsClass.isCompanion) {
                        owner.name.asString()
                    } else {
                        TODO("Not implemented yet for non top-level or companion object's member!")
                    }
                }
                else -> TODO("Not implemented yet for node $owner!")
            }
        }
    }
}
