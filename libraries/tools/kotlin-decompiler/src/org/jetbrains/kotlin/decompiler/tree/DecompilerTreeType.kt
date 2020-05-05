/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.declarations.AbstractDecompilerTreeClass
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeClass
import org.jetbrains.kotlin.decompiler.tree.declarations.name
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.toKotlinType

interface DecompilerTreeType : SourceProducible {
    val irType: IrType
    var typeClassIfExists: AbstractDecompilerTreeClass?
}

class DecompilerTreeSimpleType(
    override val irType: IrType,
    override var typeClassIfExists: AbstractDecompilerTreeClass?
) :
    DecompilerTreeType {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(typeClassIfExists?.nameIfExists ?: irType.toKotlinType().toString())
    }
}

class DecompilerTreeFunctionalType(override val irType: IrType, override var typeClassIfExists: AbstractDecompilerTreeClass? = null) :
    DecompilerTreeType {
    override fun produceSources(printer: SmartPrinter) {
        //TODO Looks like very bad implementation of type description collecting
        with(irType) {
            val arguments = toKotlinType().arguments
            val returnType = arguments.last().type
            val inputTypes = arguments.dropLast(1)
            printer.print(
                "${
                    inputTypes.joinToString(prefix = "(", postfix = ")") {
                        it.type.toString() + "?".takeIf { isNullable() }
                    }
                } -> $returnType"
            )
        }
    }
}