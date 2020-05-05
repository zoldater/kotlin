/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.printer.indented
import org.jetbrains.kotlin.decompiler.tree.expressions.AbstractDecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrProperty

//TODO Maybe we need different nodes for abstract val property and property without backingField
class DecompilerTreeProperty(
    override val element: IrProperty,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    private val backingField: DecompilerTreeField?,
    private val getter: DecompilerTreeCustomGetter?,
    private val setter: DecompilerTreeCustomSetter?,
) : DecompilerTreeDeclaration {
    override val annotationTarget: String = "property"
    var defaultModality: Modality = Modality.FINAL

    private val propertyFlagsOrNull: String?
        get() = with(element) {
            listOfNotNull(
                visibility.takeIf { it !in setOf(Visibilities.PUBLIC, Visibilities.LOCAL) }?.name?.toLowerCase(),
                "expect".takeIf { isExpect },
                modality.takeIf { it != defaultModality },
                "external".takeIf { isExternal },
                "const".takeIf { isConst },
                "lateinit".takeIf { isLateinit },
                "var".takeIf { isVar } ?: "val"
            ).joinToString(" ")
        }
    private val propertyTypeStringOrNull: String?
        get() = backingField?.type?.decompile() ?: getter?.returnType?.decompile()

    private val initializerStringOrNull: String?
        get() = backingField?.initializer?.decompile()?.let { "= $it" }

    private val headerWithTypeAndInitializer: String
        get() = listOfNotNull(
            propertyFlagsOrNull,
            propertyTypeStringOrNull?.let { "${element.name()}: $it" } ?: element.name(),
            initializerStringOrNull
        ).joinToString(" ")

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            println(headerWithTypeAndInitializer)
            indented {
                getter?.produceSources(this)
                setter?.produceSources(this)
            }
        }
    }
}