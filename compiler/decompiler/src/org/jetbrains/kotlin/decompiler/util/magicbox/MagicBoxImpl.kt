/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox

import org.jetbrains.kotlin.builtins.isBuiltinExtensionFunctionalType
import org.jetbrains.kotlin.decompiler.util.magicbox.info.DeclarationReferenceInfo
import org.jetbrains.kotlin.decompiler.util.magicbox.info.TypeInfo
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isKClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

class MagicBoxImpl : IMagicBox {
    private val declarationsWithNameByScopeMap = mutableMapOf<String, MutableSet<IrDeclarationWithName>>()
    private val declarationReferencesByScopeMap = mutableMapOf<String, MutableSet<DeclarationReferenceInfo>>()
    private val explicitTypesByScopeMap = mutableMapOf<String, MutableSet<TypeInfo>>()
    private var isFreshState = true

    companion object {
        private val defaultImportRegex = setOf(
            "kotlin\\.\\*"
        ).map { it.toRegex() }

    }

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
        if (!irType.isKClass()) {
            explicitTypesByScopeMap.addToValueSetOrInitializeIt(scopeList, TypeInfo(scopeList, irType))
            isFreshState = false
        }
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
                    .filterNot { it.calculatedListForImport.size <= 1 }
                    .map { it.calculatedListForImport.joinToString(".") }
            }
//        val typeImports = explicitTypesByScopeMap.values
//            .flatMap { set ->
//                set
//                    .filterNot { it.calculatedListForImport.size <= 1 }
//                    .map { it.calculatedListForImport.joinToString(".") }
//            }

        val localDeclarations = declarationsWithNameByScopeMap.values.flatten().mapNotNull { it.fqNameWhenAvailable?.asString() }

        val resultSet = mutableSetOf<String>()
        resultSet.addAll(declarationImports)
//        resultSet.addAll(typeImports)
        resultSet.removeAll(localDeclarations)
        return resultSet
            .sorted()
            .filter { it.isNotEmpty() }
            .filterNot { it.startsWith("kotlin") }
            .joinToString("\n", "\n", "\n") { "import $it" }
    }

    override fun obtainTypeDescriptionForScope(scopeList: List<String>, irType: IrType): String {
        if (!isFreshState) refreshState()
        return explicitTypesByScopeMap[scopeList.joinToString(".")]!!.find { it.irType == irType && it.scopeList == scopeList }!!.calculatedName
    }

    private fun refreshState() {
        resolveExplicitTypesConflicts()
        resolveDeclarationReferencesConflicts()
        isFreshState = true
    }

    //TODO реализовать алгоритм по резолву имен в местах вызова (Это говно, думай дальше)
    private fun resolveDeclarationReferencesConflicts() {
        for ((declScope, declRefSet) in declarationReferencesByScopeMap) {
            for (lhs in declRefSet.withIndex()) {
                for (rhs in declRefSet.withIndex()) {
                    if (lhs.index != rhs.index && lhs.value.calculatedName == rhs.value.calculatedName) {
                        rhs.value.doResolveStep()
                    }
                }
                for (declWithNameSet in declarationsWithNameByScopeMap.values) {
                    for (declWithName in declWithNameSet) {
                        if (declWithName.name() == lhs.value.calculatedName) {
                            lhs.value.doResolveStep()
                        }
                    }
                }
            }
        }
    }

    private fun resolveExplicitTypesConflicts() {
        for ((typeScope, typeInfoSet) in explicitTypesByScopeMap) {
            for (lhsTypeInfo in typeInfoSet.withIndex()) {
                for (rhsTypeInfo in typeInfoSet.withIndex()) {
                    if (lhsTypeInfo.index != rhsTypeInfo.index && lhsTypeInfo.value.calculatedName == rhsTypeInfo.value.calculatedName) {
                        rhsTypeInfo.value.doResolveStep()
                    }
                }
            }
        }
    }


    private fun <T> MutableMap<String, MutableSet<T>>.addToValueSetOrInitializeIt(scopeList: List<String>, element: T) {
        if (containsKey(scopeList.joinToString("."))) {
            this[scopeList.joinToString(".")]?.add(element)
        } else {
            this[scopeList.joinToString(".")] = mutableSetOf(element)
        }
    }
}