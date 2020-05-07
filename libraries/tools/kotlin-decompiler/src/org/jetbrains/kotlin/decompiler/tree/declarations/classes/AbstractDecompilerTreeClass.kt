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
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class AbstractDecompilerTreeClass(
    final override val element: IrClass,
    override val declarations: List<DecompilerTreeDeclaration>,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    val superTypes: List<DecompilerTreeType>,
    override val annotationTarget: String? = null,
) : DecompilerTreeDeclaration,
    DecompilerTreeDeclarationContainer,
    DecompilerTreeTypeParametersContainer {
    open val isDefaultModality: Boolean = element.modality == Modality.FINAL
    abstract val keyword: String
    abstract val thisReceiver: AbstractDecompilerTreeValueParameter?

    protected open val nonTrivialSuperInterfaces: List<DecompilerTreeType>
        get() = superTypes.filterNot { it.irType.isAny() || it.irType.isUnit() }
            .filterNot { it.typeClassIfExists is DecompilerTreeClass }

    protected open val primaryConstructor: AbstractDecompilerTreeConstructor?
        get() = declarations.filterIsInstance(DecompilerTreePrimaryConstructor::class.java)
            .firstOrNull()

    protected open val secondaryConstructors: List<DecompilerTreeSecondaryConstructor>
        get() = declarations.filterIsInstance(DecompilerTreeSecondaryConstructor::class.java)

    protected open val initSections: List<DecompilerTreeAnonymousInitializer>
        get() = declarations.filterIsInstance(DecompilerTreeAnonymousInitializer::class.java)

    protected open val properties: List<DecompilerTreeProperty>
        get() = declarations.filterIsInstance(DecompilerTreeProperty::class.java).filterNot { it.element.isFakeOverride }

    protected open val methods: List<DecompilerTreeSimpleFunction>
        get() = declarations.filterIsInstance(DecompilerTreeSimpleFunction::class.java).filterNot {
            it.element.isFakeOverride
        }

    protected open val otherPrintableDeclarations: List<DecompilerTreeDeclaration>
        get() = declarations.asSequence()
            .filterNot { it is AbstractDecompilerTreeConstructor }
            .filterNot { it is DecompilerTreeAnonymousInitializer }
            .filterNot { it is DecompilerTreeProperty }
            .filterNot { it is DecompilerTreeSimpleFunction }
            .filterNot { it.element?.isFakeOverride ?: false }
            .toList()

    open val printableDeclarations: List<DecompilerTreeDeclaration> = emptyList()

    protected val computeModifiersAndName: String
        get() = with(element) {
            return listOfNotNull(
                visibility.name.toLowerCase().takeIf { element.visibility != Visibilities.PUBLIC },
                "expect".takeIf { isExpect }, //actual modifier?
                modality.name.toLowerCase().takeIf { !isDefaultModality },
                "external".takeIf { isExternal },
                "inner".takeIf { isInner },
                "inline".takeIf { isInline },
                "companion".takeIf { isCompanion },
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
                        it.produceSources(this@with)
                    }
                }
            } ?: println()
        }
    }
}

