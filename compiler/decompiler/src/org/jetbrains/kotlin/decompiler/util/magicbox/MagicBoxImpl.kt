/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox

import org.jetbrains.kotlin.decompiler.util.magicbox.info.DeclarationReferenceInfo
import org.jetbrains.kotlin.decompiler.util.magicbox.info.TypeInfo
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.types.IrType

class MagicBoxImpl : IMagicBox {
    private val declarationsWithNameByScopeMap = mutableMapOf<String, MutableSet<IrDeclarationWithName>>()
    private val declarationReferencesByScopeMap = mutableMapOf<String, MutableSet<DeclarationReferenceInfo>>()
    private val explicitTypesByScopeMap = mutableMapOf<String, MutableSet<TypeInfo>>()
    private var isFreshState = true

    override fun putDeclarationWithName(scopeList: List<String>, irDeclarationWithName: IrDeclarationWithName) {
        //Пока только кладем в коробку и врубаем тригер на обновление
        declarationsWithNameByScopeMap.addToValueSetOrInitializeIt(scopeList, irDeclarationWithName)
        isFreshState = false
    }

    override fun putCalledDeclarationReferenceWithScope(scopeList: List<String>, irDeclarationReference: IrDeclarationReference) {
        //Добавляем в маппинг на скоуп
        declarationReferencesByScopeMap.addToValueSetOrInitializeIt(scopeList, DeclarationReferenceInfo(scopeList, irDeclarationReference))
        isFreshState = false
    }

    override fun putExplicitTypeWithScope(scopeList: List<String>, irType: IrType) {
        //Добавляем в маппинг на скоуп
        explicitTypesByScopeMap.addToValueSetOrInitializeIt(scopeList, TypeInfo(scopeList, irType))
        isFreshState = false
    }

    override fun obtainDeclarationReferenceDescription(scopeList: List<String>, irDeclarationReference: IrDeclarationReference): String {
        if (!isFreshState) refreshState()
        return declarationReferencesByScopeMap[scopeList.joinToString(".")]!!
            .find { it.irDeclarationReference == irDeclarationReference && it.scopeList == scopeList }!!
            .calculatedName
    }

    override fun obtainImportStatementsList(): String {
        if (!isFreshState) refreshState()
        val declarationImports = declarationReferencesByScopeMap.values
            .flatMap { set ->
                set
                    .map { it.calculatedListForImport.joinToString(".") }
            }
        val typeImports = explicitTypesByScopeMap.values
            .flatMap { set ->
                set
                    .map { it.calculatedListForImport.joinToString(".") }
            }

        val resultSet = mutableSetOf<String>()
        resultSet.addAll(declarationImports)
        resultSet.addAll(typeImports)
        return resultSet.sorted().joinToString("\n", "\n", "\n") { "import $it" }
    }

    override fun obtainTypeDescriptionForScope(scopeList: List<String>, irType: IrType): String {
        if (!isFreshState) refreshState()
        return explicitTypesByScopeMap[scopeList.joinToString(".")]!!.find { it.irType == irType && it.scopeList == scopeList }!!.calculatedName
    }

    private fun refreshState() {
        resolveDeclarationReferencesConflicts()
        resolveExplicitTypesConflicts()
        isFreshState = true
    }

    //TODO реализовать алгоритм по резолву имен в местах вызова (Это говно, думай дальше)
    private fun resolveDeclarationReferencesConflicts() {
        for ((scope, set) in declarationReferencesByScopeMap) {
            for (lhs in set.withIndex()) {
                for (rhs in set.withIndex()) {
                    if (lhs.index != rhs.index && lhs.value.calculatedName == rhs.value.calculatedName) {
                        with(rhs.value) {
                            calculatedName =
                                (calculatedListForImport.subList(0, calculatedListForImport.size - 1) + calculatedName).joinToString(".")
                            calculatedListForImport = listOf()
                        }
                    }
                }
            }
        }
    }

    private fun resolveExplicitTypesConflicts() {

    }


    private fun <T> MutableMap<String, MutableSet<T>>.addToValueSetOrInitializeIt(scopeList: List<String>, element: T) {
        if (containsKey(scopeList.joinToString("."))) {
            this[scopeList.joinToString(".")]?.add(element)
        } else {
            this[scopeList.joinToString(".")] = mutableSetOf(element)
        }
    }
}