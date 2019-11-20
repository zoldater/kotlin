/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.types.IrType

interface IMagicBox {
    /**
     * В Import Resolver'е вызываем в месте объявления классов, конструкторов, Enum Entry, пропертей, функций, typealias'ов
     * - т.е. всего, что может быть импортированным, на ноде соответствующего объявления
     * Учитывается для резолва конфликтов имен и вычета из import statements
     */
    fun putDeclarationWithName(irDeclarationWithName: IrDeclarationWithName)

    /**
     * В Import Resolver'е в местах вызова конструкторов классов, делегированных вызовов конструкторов классов,
     * конструкторов Enum'ов, функций
     */
    fun putCalledDeclarationReference(scopeList: List<String>, irDeclarationReference: IrDeclarationReference)

    /**
     * 
     */
    fun obtainDeclarationReferenceDescription(scopeList: List<String>, irDeclarationReference: IrDeclarationReference)

    fun obtainImportStatementsList(): List<String>
    fun obtainTypeDescriptionForScope(scopeList: List<String>, irType: IrType): String
}