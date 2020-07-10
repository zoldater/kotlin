/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeDeclarationContainer
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeTypeParametersContainer
import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class AbstractDecompilerTreeClass(
    final override val element: IrClass,
    override val declarations: List<DecompilerTreeDeclaration>,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    val superTypes: List<DecompilerTreeType>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    val thisReceiver: AbstractDecompilerTreeValueParameter?,
    override val annotationTarget: String? = null,
) : DecompilerTreeDeclaration,
    DecompilerTreeDeclarationContainer,
    DecompilerTreeTypeParametersContainer {

    constructor(configuration: DecompilerTreeClassConfiguration) : this(
        configuration.element,
        configuration.declarations,
        configuration.annotations,
        configuration.superTypes,
        configuration.typeParameters,
        configuration.thisReceiver
    )

    open val isDefaultModality: Boolean = element.modality == Modality.FINAL
    abstract val keyword: String

    protected open val nonTrivialSuperInterfaces: List<DecompilerTreeType>
        get() = superTypes.filterNot { it.irType.isAny() || it.irType.isUnit() }
            .filterNot { (it.typeClassIfExists is DecompilerTreeClass).takeIf { primaryConstructor != null } ?: false }

    internal open val primaryConstructor: AbstractDecompilerTreeConstructor?
        get() = declarations.filterIsInstance<DecompilerTreePrimaryConstructor>().firstOrNull()

    internal open val secondaryConstructors: List<DecompilerTreeSecondaryConstructor>
        get() = declarations.filterIsInstance<DecompilerTreeSecondaryConstructor>()

    internal open val initSections: List<DecompilerTreeAnonymousInitializer>
        get() = declarations.filterIsInstance<DecompilerTreeAnonymousInitializer>()

    internal open val properties: List<DecompilerTreeProperty>
        get() = declarations.filterIsInstance<DecompilerTreeProperty>().filterNot { it.element.isFakeOverride }

    internal open val methods: List<DecompilerTreeSimpleFunction>
        get() = declarations.filterIsInstance<DecompilerTreeSimpleFunction>()
            .filterNot { it.element.isFakeOverride || it.element.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER }

    protected open val otherPrintableDeclarations: List<DecompilerTreeDeclaration>
        get() = declarations.asSequence()
            .filterNot { it is AbstractDecompilerTreeConstructor }
            .filterNot { it is DecompilerTreeAnonymousInitializer }
            .filterNot { it is DecompilerTreeProperty }
            .filterNot { it is DecompilerTreeSimpleFunction }
            .filterNot { it.element?.isFakeOverride ?: false }
            .toList()

    abstract val printableDeclarations: List<DecompilerTreeDeclaration>

    protected open val computeModifiersAndName: String
        get() = with(element) {
            return listOfNotNull(
                visibility.name.toLowerCase().takeIf { element.visibility != Visibilities.PUBLIC },
                "expect".takeIf { isExpect }, //actual modifier?
                modality.name.toLowerCase().takeIf { !isDefaultModality }
                    ?.also { properties.forEach { p -> p.defaultModality = modality } },
                "external".takeIf { isExternal },
                "inner".takeIf { isInner },
                "inline".takeIf { isInline },
                "companion".takeIf { isCompanion },
                "data".takeIf { isData },
                keyword,
                nameIfExists
            ).joinToString(separator = " ")
        }

    protected open val nameWithPrimaryCtorDecompiled: String?
        get() = listOfNotNull(
            computeModifiersAndName,
            typeParametersForPrint,
            primaryConstructor?.decompile()
        ).ifNotEmpty { joinToString("") }

    private val nonTrivialSuperTypesDecompiledOrNull: String?
        get() = nonTrivialSuperInterfaces.ifNotEmpty { joinToString { it.decompile() } }

    protected val fullHeader: String
        get() = listOfNotNull(
            nameWithPrimaryCtorDecompiled,
            nonTrivialSuperTypesDecompiledOrNull?.let {
                primaryConstructor?.delegatingCallDecompiledOrNull?.let { _ -> ", $it" } ?: ": $it"
            }
        ).joinToString(" ").trimEnd()

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print(fullHeader)
            printableDeclarations.ifNotEmpty {
                this@with.withBraces {
                    this.forEach {
                        it.decompile().lines().filter { l -> l.isNotBlank() }.forEach { l -> this@with.println(l) }
                    }
                }
            } ?: println()
        }
    }
}

data class DecompilerTreeClassConfiguration(
    val element: IrClass,
    val declarations: List<DecompilerTreeDeclaration>,
    val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    val superTypes: List<DecompilerTreeType>,
    val thisReceiver: AbstractDecompilerTreeValueParameter?,
    val typeParameters: List<DecompilerTreeTypeParameter>,
    val annotationTarget: String? = null,
)

class DecompilerTreeAnonymousClass(configuration: DecompilerTreeClassConfiguration) : AbstractDecompilerTreeClass(configuration) {
    init {
        primaryConstructor?.isObjectConstructor = true
    }

    override val keyword: String
        get() = throw UnsupportedOperationException("Anonymous class hasn't name and power!")

    override val computeModifiersAndName: String = "object"

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, initSections, secondaryConstructors, methods, otherPrintableDeclarations).flatten()

}