/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

class DecompilerTreeDataClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: AbstractDecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {

    override val keyword: String = "data class"

    override val primaryConstructor: DecompilerTreeDataClassPrimaryConstructor?
        get() = declarations.filterIsInstance(DecompilerTreeDataClassPrimaryConstructor::class.java).firstOrNull()

    private val primaryConstructorPropertiesOrNull
        get() = primaryConstructor?.valueParameters?.mapNotNull { it as? DecompilerTreePropertyValueParameter }
            ?.mapNotNull { it.relatedProperty }

    override val methods: List<DecompilerTreeSimpleFunction>
        get() = super.methods.filterNot { it.element.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER }

    override val properties: List<DecompilerTreeProperty>
        get() = primaryConstructorPropertiesOrNull?.let { super.properties - it } ?: super.properties

    private fun pinPropertiesToValueDeclarations() {
        primaryConstructor?.valueParameters?.mapNotNull { it as? DecompilerTreePropertyValueParameter }
            ?.forEach {
                properties.find { p -> p.nameIfExists == it.nameIfExists }
                    ?.also { p ->
                        it.relatedProperty = p
                    }
            }
    }

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = (properties + methods + otherPrintableDeclarations)

    override fun produceSources(printer: SmartPrinter) {
        pinPropertiesToValueDeclarations()
        super.produceSources(printer)
    }
}