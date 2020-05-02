/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeDeclarationContainer
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrClass

abstract class AbstractDecompilerTreeClass(
    final override val element: IrClass,
    override val declarations: List<DecompilerTreeDeclaration>,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val annotationTarget: String? = null
) : DecompilerTreeDeclaration, DecompilerTreeDeclarationContainer {
    open val isDefaultModality: Boolean = element.modality == Modality.FINAL
    abstract val keyword: String

    fun computeModifiersAndName(): String {
        val isDefaultVisibility = element.visibility == Visibilities.PUBLIC

        with(element) {
            return listOfNotNull(
                visibility.name.toLowerCase().takeIf { !isDefaultVisibility },
                "expect".takeIf { isExpect }, //actual modifier?
                modality.name.takeIf { !isDefaultModality },
                "external".takeIf { isExternal },
                "inner".takeIf { isInner },
                "inline".takeIf { isInline },
                "data".takeIf { isData },
                keyword,
                nameIfExists
            ).joinToString(separator = " ")
        }
    }
}

class DecompilerTreeInterface(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>
) : AbstractDecompilerTreeClass(element, declarations, annotations) {
    override val keyword: String = "interface"
    override val isDefaultModality: Boolean = element.modality == Modality.OPEN

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>
) : AbstractDecompilerTreeClass(element, declarations, annotations) {
    override val keyword: String = "class"

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeEnumClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>
) : AbstractDecompilerTreeClass(element, declarations, annotations) {
    override val keyword: String = "enum class"

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeAnnotationClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>
) : AbstractDecompilerTreeClass(element, declarations, annotations) {
    override val keyword: String = "annotation class"

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeObject(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>
) : AbstractDecompilerTreeClass(element, declarations, annotations) {
    override val keyword: String = "object"
    override val nameIfExists: String? = element.name().takeIf { it != "<no name provided>" }

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}