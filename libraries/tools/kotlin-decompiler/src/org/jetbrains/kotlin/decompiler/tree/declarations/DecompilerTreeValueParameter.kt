/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.AbstractDecompilerTreeExpressionBody
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter

interface AbstractDecompilerTreeValueParameter : DecompilerTreeValueDeclaration {
    override val element: IrValueParameter
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>
    var defaultValue: AbstractDecompilerTreeExpressionBody?
    override val type: DecompilerTreeType
    val varargType: DecompilerTreeType?
    override val annotationTarget: String?

    override fun produceSources(printer: SmartPrinter) {
        with(element) {
            listOfNotNull(
                "vararg".takeIf { varargElementType != null },
                "crossinline".takeIf { isCrossinline },
                "noinline".takeIf { isNoinline },
                nameIfExists?.let { "$it:" },
                varargType?.decompile() ?: this@AbstractDecompilerTreeValueParameter.type.decompile(),
                this@AbstractDecompilerTreeValueParameter.defaultValue?.let { "= ${it.decompile()}" }
            ).joinToString(" ").also { printer.print(it) }
        }
    }
}

class DecompilerTreePropertyValueParameter(
    override val element: IrValueParameter,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var defaultValue: AbstractDecompilerTreeExpressionBody?,
    override val type: DecompilerTreeType,
    override val varargType: DecompilerTreeType? = null,
    override val annotationTarget: String? = null
) : AbstractDecompilerTreeValueParameter {
    var relatedProperty: DecompilerTreeProperty? = null

    override fun produceSources(printer: SmartPrinter) {
        relatedProperty?.also {
            val declaration = listOfNotNull(
                listOfNotNull(
                    it.propertyFlagsOrNull, it.nameIfExists
                ).joinToString(" "),
                it.propertyTypeStringOrNull?.let { ": $it" }
            ).joinToString("")
            val initializer = defaultValue?.decompile()?.let { dv -> "= $dv" }
            listOfNotNull(declaration, initializer).joinToString(" ").also { full ->
                printer.print(full)
            }
        } ?: super.produceSources(printer)
    }
}

class DecompilerTreeValueParameter(
    override val element: IrValueParameter,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var defaultValue: AbstractDecompilerTreeExpressionBody?,
    override val type: DecompilerTreeType,
    override val varargType: DecompilerTreeType? = null,
    override val annotationTarget: String? = null
) : AbstractDecompilerTreeValueParameter {
}