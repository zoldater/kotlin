/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.printer.indented
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeTypeParametersContainer
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.isTopLevelDeclaration

//TODO Maybe we need different nodes for abstract val property and property without backingField
class DecompilerTreeProperty(
    override val element: IrProperty,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    private val backingField: DecompilerTreeField?,
    private val getter: DecompilerTreeCustomGetter?,
    private val setter: DecompilerTreeCustomSetter?,
    private val isDeclaredInConstructor: Boolean = false,
) : DecompilerTreeDeclaration, DecompilerTreeTypeParametersContainer {
    override val annotationTarget: String = "property"
    var defaultModality: Modality = Modality.FINAL

    override val typeParameters: List<DecompilerTreeTypeParameter>
        get() = getter?.typeParameters ?: emptyList()

    private val isDelegated: Boolean
        get() = backingField?.element?.origin == IrDeclarationOrigin.PROPERTY_DELEGATE

    internal
    val propertyFlagsOrNull: String?
        get() = with(element) {
            listOfNotNull(
                visibility.takeIf { it !in setOf(Visibilities.PUBLIC, Visibilities.LOCAL) }?.name?.toLowerCase(),
                "expect".takeIf { isExpect },
                modality.takeIf { !element.isTopLevelDeclaration }?.name?.toLowerCase(),
                "external".takeIf { isExternal },
                "override".takeIf { this@DecompilerTreeProperty.getter?.isOverriden() ?: false },
                "const".takeIf { isConst },
                "lateinit".takeIf { isLateinit },
                "var".takeIf { isVar } ?: "val",
                typeParametersForPrint
            ).joinToString(" ")
        }
    internal val propertyTypeStringOrNull: String?
        get() = getter?.returnType?.decompile() ?: backingField?.type?.decompile()

    private val initializerStringOrNull: String?
        get() = backingField?.decompile()
            ?.let {
                if (isDelegated) "by $it" else "= $it"
            }

    private val nameWithReciever
        get() = (getter?.extensionReceiverParameter?.type?.decompile()?.let { "$it." } ?: "") + nameIfExists

    private val headerWithTypeAndInitializer: String
        get() = listOfNotNull(
            propertyFlagsOrNull,
            nameWithReciever.let {
                propertyTypeStringOrNull?.let { type -> "$it: $type" } ?: it
            },
            initializerStringOrNull
        ).joinToString(" ")

    override fun produceSources(printer: SmartPrinter) {
        if (isDeclaredInConstructor) return

        with(printer) {
            println(headerWithTypeAndInitializer)
            indented {
                getter?.takeIf { !isDelegated }?.produceSources(this)
                setter?.takeIf { !isDelegated }?.produceSources(this)
            }
        }
    }
}
