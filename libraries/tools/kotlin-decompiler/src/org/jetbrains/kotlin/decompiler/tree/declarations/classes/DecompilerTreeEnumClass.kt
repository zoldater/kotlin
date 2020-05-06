/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class DecompilerTreeEnumClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: AbstractDecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "enum class"

    private val enumEntries: List<DecompilerTreeEnumEntry>
        get() = super.otherPrintableDeclarations.filterIsInstance(DecompilerTreeEnumEntry::class.java)

    override val methods: List<DecompilerTreeSimpleFunction>
        get() = super.methods.filterNot { it.element.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER }

    override val otherPrintableDeclarations: List<DecompilerTreeDeclaration>
        get() = super.otherPrintableDeclarations.filterNot { it is DecompilerTreeEnumEntry }

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOfNotNull(properties, methods, otherPrintableDeclarations).flatten()

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print(computeModifiersAndName)
            primaryConstructor?.valueParameters?.ifNotEmpty { print(primaryConstructor!!.valueParametersForPrint) }
            enumEntries.forEach { it.enumClassName = element.fqNameWhenAvailable?.asString() }

            (printableDeclarations + enumEntries).ifNotEmpty {
                this@with.withBraces {
                    val decompiledEntries = enumEntries.map { it.decompile() }

                    primaryConstructor?.valueParameters?.ifNotEmpty {
                        decompiledEntries.joinToString(",\n", postfix = ";").lines().forEach { println(it) }
                    } ?: decompiledEntries.joinToString(", ", postfix = ";").also { println(it) }

                    printableDeclarations.forEach {
                        it.produceSources(this@with)
                    }
                }
            } ?: println()

        }
    }
}