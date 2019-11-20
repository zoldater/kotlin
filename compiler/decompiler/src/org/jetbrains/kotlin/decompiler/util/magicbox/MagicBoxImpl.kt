/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox

import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.parentAsClass

class MagicBoxImpl : IMagicBox {
    private val localDeclarationsWithNameSet = mutableMapOf<String, MutableSet<IrDeclarationWithName>>()
    private val scopeWithDeclarationReferencesMap = mutableMapOf<String, MutableSet<IrDeclarationReference>>()
    private val scopeWithExplicitTypesMap = mutableMapOf<String, MutableSet<IrType>>()
    //    private val
    private var isFreshState = true

    override fun putDeclarationWithName(scopeList: List<String>, irDeclarationWithName: IrDeclarationWithName) {
        //Пока только кладем в коробку и врубаем тригер на обновление
        localDeclarationsWithNameSet.addToValueSetOrInitializeIt(scopeList, irDeclarationWithName)
        isFreshState = false
    }

    override fun putCalledDeclarationReferenceWithScope(scopeList: List<String>, irDeclarationReference: IrDeclarationReference) {
        //Добавляем в маппинг на скоуп
        scopeWithDeclarationReferencesMap.addToValueSetOrInitializeIt(scopeList, irDeclarationReference)
        isFreshState = false
    }

    override fun putExplicitTypeWithScope(scopeList: List<String>, irType: IrType) {
        //Добавляем в маппинг на скоуп
        scopeWithExplicitTypesMap.addToValueSetOrInitializeIt(scopeList, irType)
        isFreshState = false
    }

    override fun obtainDeclarationReferenceDescription(scopeList: List<String>, irDeclarationReference: IrDeclarationReference) {
        if (!isFreshState) refreshState()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun obtainImportStatementsList(): List<String> {
        if (!isFreshState) refreshState()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun obtainTypeDescriptionForScope(scopeList: List<String>, irType: IrType): String {

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //TODO реализовать алгоритм по резолву
    private fun refreshState() {
    }

    private fun <T> MutableMap<String, MutableSet<T>>.addToValueSetOrInitializeIt(scopeList: List<String>, element: T) {
        if (containsKey(scopeList.joinToString("."))) {
            this[scopeList.joinToString(".")]?.add(element)
        } else {
            this[scopeList.joinToString(".")] = mutableSetOf(element)
        }
    }

    /**
     * Используем для маппинга DeclarationReference, вызываемого в соответствующем скоупе, в строковое представление,
     * используемое для его отображения в генерируемом исходном коде, и информацию, необходимую для его импорта
     */
    private class CalledDeclarationInfo(val scopeList: List<String>, val irDeclarationReference: IrDeclarationReference) {
        private val calculatedListForImport = mutableListOf<String>()
        private val calculatedListForRender = mutableListOf(irDeclarationReference.name())

        private fun <T : IrDeclarationReference> T.name(): String {
            return when (val owner = symbol.owner) {
                is IrProperty -> owner.name.asString()
                is IrConstructor -> owner.parentAsClass.name()
                //Если функция - топ-левел или объявлена внутри Companion object, то ее следует импортировать, а строковое представление
                //формируется нетипичным образом, как:
                // <Имя класса (для функции из безымянного companion) / object'а / пакета (для топ-левел)> + "." + <>
                is IrSimpleFunction

            }
        }
    }
}