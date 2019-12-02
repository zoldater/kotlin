/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox

import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType

interface IMagicBox {
    /**
     * В Import Resolver'е вызываем в месте объявления классов, конструкторов, Enum Entry, пропертей, функций, typealias'ов
     * - т.е. всего, что может быть импортированным, на ноде соответствующего объявления
     * Учитывается для резолва конфликтов имен и вычета из import statements
     */
    fun putDeclarationWithName(scopeList: List<String>, irDeclarationWithName: IrDeclarationWithName)

    /**
     * В Import Resolver'е в местах вызова конструкторов классов, делегированных вызовов конструкторов классов,
     * конструкторов Enum'ов, функций
     */
    fun putCalledDeclarationReferenceWithScope(scopeList: List<String>, irDeclarationReference: IrDeclarationReference)

    fun putExplicitTypeWithScope(scopeList: List<String>, irSimpleType: IrSimpleType)

    /**
     *
     */
    fun obtainDeclarationReferenceDescription(scopeList: List<String>, irDeclarationReference: IrDeclarationReference): String

    fun obtainImportStatementsList(): String
    fun obtainTypeDescriptionForScope(scopeList: List<String>, irSimpleType: IrSimpleType): String
}