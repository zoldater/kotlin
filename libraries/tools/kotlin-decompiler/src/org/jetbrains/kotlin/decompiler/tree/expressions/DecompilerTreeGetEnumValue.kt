/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.AbstractDecompilerTreeClass
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeEnumEntry
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue

class DecompilerTreeGetEnumValue(
    override val element: IrGetEnumValue,
    val parentDeclaration: DecompilerTreeEnumEntry,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        //TODO replace fqName with short name
        //TODO try to find out NPE cases
        parentDeclaration.nameIfExists?.also { ownName ->
            parentDeclaration.enumClasName?.also {
                printer.print("$it.$ownName")
            }
        }
    }
}

class DecompilerTreeGetObjectValue(
    override val element: IrGetObjectValue,
    val parentDeclaration: AbstractDecompilerTreeClass,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        //TODO replace fqName with short name
        //TODO try to find out NPE cases
        parentDeclaration.nameIfExists?.also { printer.print(it) }
    }
}