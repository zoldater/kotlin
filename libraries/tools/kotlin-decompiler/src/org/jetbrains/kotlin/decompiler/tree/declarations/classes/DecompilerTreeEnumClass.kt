/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeEnumEntry
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeSimpleFunction
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class DecompilerTreeEnumClass(configuration: DecompilerTreeClassConfiguration) : AbstractDecompilerTreeClass(configuration) {
    override val keyword: String = "enum class"

    private val enumEntries: List<DecompilerTreeEnumEntry>
        get() = super.declarations.filterIsInstance<DecompilerTreeEnumEntry>()

    override val methods: List<DecompilerTreeSimpleFunction>
        get() = super.methods.filterNot { it.element.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER }

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print(computeModifiersAndName)
            primaryConstructor?.valueParameters?.ifNotEmpty { print(primaryConstructor!!.valueParametersForPrint) }
            enumEntries.forEach { it.enumClassName = element.fqNameWhenAvailable?.asString() }

            (declarationsToPrint + enumEntries).ifNotEmpty {
                this@with.withBraces {
                    val decompiledEntries = enumEntries.map { it.decompile() }

                    primaryConstructor?.valueParameters?.ifNotEmpty {
                        decompiledEntries.joinToString(",\n", postfix = ";").lines().forEach { println(it) }
                    } ?: decompiledEntries.joinToString(", ", postfix = ";").also { println(it) }

                    declarationsToPrint.forEach {
                        it.produceSources(this@with)
                    }
                }
            } ?: println()

        }
    }
}