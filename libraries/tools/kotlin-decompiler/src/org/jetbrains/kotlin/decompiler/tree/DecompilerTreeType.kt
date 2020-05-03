/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isLocalClass

internal fun IrType.buildType(): DecompilerTreeType =
    when {
        toKotlinType().isFunctionTypeOrSubtype -> DecompilerTreeFunctionalType(this)
        classOrNull?.owner?.isLocalClass() ?: false -> DecompilerTreeLocalClassType(this)
        else -> DecompilerTreeSimpleType(this)
    }


interface DecompilerTreeType : SourceProducible {
    val irType: IrType
}

class DecompilerTreeSimpleType(override val irType: IrType) : DecompilerTreeType {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(irType.toKotlinType().toString())
    }
}

class DecompilerTreeFunctionalType(override val irType: IrType) : DecompilerTreeType {
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

class DecompilerTreeLocalClassType(override val irType: IrType) : DecompilerTreeType {
    override fun produceSources(printer: SmartPrinter) {
        printer.print(irType.getClass()!!.name())
    }
}

