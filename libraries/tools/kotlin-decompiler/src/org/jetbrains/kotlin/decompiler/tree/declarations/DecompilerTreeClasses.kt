/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeDeclarationContainer
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeTypeParametersContainer
import org.jetbrains.kotlin.decompiler.tree.expressions.AbstractDecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class AbstractDecompilerTreeClass(
    final override val element: IrClass,
    override val declarations: List<DecompilerTreeDeclaration>,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    val superTypes: List<DecompilerTreeType>,
    override val annotationTarget: String? = null,
) : DecompilerTreeDeclaration, DecompilerTreeDeclarationContainer, DecompilerTreeTypeParametersContainer {
    open val isDefaultModality: Boolean = element.modality == Modality.FINAL
    abstract val keyword: String
    abstract val thisReceiver: DecompilerTreeValueParameter?

    protected open val nonTrivialSuperInterfaces: List<DecompilerTreeType>
        get() = superTypes.filterNot { it.irType.isAny() || it.irType.isUnit() }
            .filterNot { it.typeClassIfExists is DecompilerTreeClass }

    protected val primaryConstructor: DecompilerTreePrimaryConstructor?
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
            it.element.isFakeOverride || it.element.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER
        }

    protected open val otherPrintableDeclarations: List<DecompilerTreeDeclaration>
        get() = declarations.asSequence()
            .filterNot { it is AbstractDecompilerTreeConstructor }
            .filterNot { it is DecompilerTreeAnonymousInitializer }
            .filterNot { it is DecompilerTreeProperty }
            .filterNot { it is DecompilerTreeSimpleFunction }
            .filterNot { it.element.isFakeOverride }
            .toList()

    abstract val printableDeclarations: List<DecompilerTreeDeclaration>

    protected val computeModifiersAndName: String
        get() = with(element) {
            return listOfNotNull(
                visibility.name.toLowerCase().takeIf { element.visibility != Visibilities.PUBLIC },
                "expect".takeIf { isExpect }, //actual modifier?
                modality.name.toLowerCase().takeIf { !isDefaultModality },
                "external".takeIf { isExternal },
                "inner".takeIf { isInner },
                "inline".takeIf { isInline },
                "data".takeIf { isData },
                keyword,
                nameIfExists,
                typeParametersForPrint
            ).joinToString(separator = " ")
        }

    protected open val nameWithPrimaryCtorDecompiled: String?
        get() = listOfNotNull(computeModifiersAndName, primaryConstructor?.decompile()).ifNotEmpty { joinToString("") }

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


class DecompilerTreeInterface(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override val typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "interface"

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, methods, otherPrintableDeclarations).flatten()

    override val isDefaultModality: Boolean = element.modality == Modality.ABSTRACT

    init {
        properties.forEach { it.defaultModality = Modality.ABSTRACT }
        methods.forEach { it.defaultModality = Modality.ABSTRACT }
    }
}

class DecompilerTreeAnnotationClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "annotation class"

    override val nonTrivialSuperInterfaces: List<DecompilerTreeType>
        get() = super.nonTrivialSuperInterfaces.filterNot { it.decompile().toLowerCase() == "annotation" }

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, initSections, secondaryConstructors, methods, otherPrintableDeclarations).flatten()

}

class DecompilerTreeClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "class"

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, initSections, secondaryConstructors, methods, otherPrintableDeclarations).flatten()

}

class DecompilerTreeEnumClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
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
            enumEntries.forEach { it.enumClasName = nameIfExists }

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

class DecompilerTreeObject(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {

    override val keyword: String = "object"

    //    override val nameWithPrimaryCtorDecompiled: String = "$computeModifiersAndName ${element.name()}"

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, initSections, methods, otherPrintableDeclarations).flatten()

    override val nameIfExists: String? = element.name().takeIf { it != "<no name provided>" }

    override val nameWithPrimaryCtorDecompiled: String?
        get() = computeModifiersAndName
}

internal fun buildClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    typeParameters: List<DecompilerTreeTypeParameter>,
    thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
): AbstractDecompilerTreeClass = when (element.kind) {
    INTERFACE -> DecompilerTreeInterface(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
    ENUM_CLASS -> DecompilerTreeEnumClass(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
    ANNOTATION_CLASS -> DecompilerTreeAnnotationClass(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
    //TODO is it enough for `object SomeObj` val x = object : Any {...}
    OBJECT -> DecompilerTreeObject(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
    else -> DecompilerTreeClass(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
}

