/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeDeclarationContainer
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeTypeParametersContainer
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.decompiler.util.withBraces
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.isAny

abstract class AbstractDecompilerTreeClass(
    final override val element: IrClass,
    override val declarations: List<DecompilerTreeDeclaration>,
    override val annotations: List<DecompilerTreeConstructorCall>,
    val superTypes: List<DecompilerTreeType>,
    override val annotationTarget: String? = null
) : DecompilerTreeDeclaration, DecompilerTreeDeclarationContainer, DecompilerTreeTypeParametersContainer {
    open val isDefaultModality: Boolean = element.modality == Modality.FINAL
    abstract val keyword: String
    abstract val thisReceiver: DecompilerTreeValueParameter?


    protected val primaryConstructor: DecompilerTreeConstructor?
        get() = declarations.filterIsInstance(DecompilerTreeConstructor::class.java).find { it.element.isPrimary }

    protected val secondaryConstructors: List<DecompilerTreeConstructor>
        get() = declarations.filterIsInstance(DecompilerTreeConstructor::class.java).filterNot { it.element.isPrimary }

    protected val initSections: List<DecompilerTreeAnonymousInitializer>
        get() = declarations.filterIsInstance(DecompilerTreeAnonymousInitializer::class.java)

    protected val properties: List<DecompilerTreeProperty>
        get() = declarations.filterIsInstance(DecompilerTreeProperty::class.java).filterNot { it.element.isFakeOverride }

    protected val methods: List<DecompilerTreeSimpleFunction>
        get() = declarations.filterIsInstance(DecompilerTreeSimpleFunction::class.java).filterNot { it.element.isFakeOverride }

    protected val otherPrintableDeclarations: List<DecompilerTreeDeclaration>
        get() = declarations.filterNot { it is DecompilerTreeConstructor }
            .filterNot { it in initSections }
            .filterNot { it in properties }
            .filterNot { it in methods }

    val computeModifiersAndName: String
        get() = with(element) {
            return listOfNotNull(
                visibility.name.toLowerCase().takeIf { element.visibility != Visibilities.PUBLIC },
                "expect".takeIf { isExpect }, //actual modifier?
                modality.name.takeIf { !isDefaultModality },
                "external".takeIf { isExternal },
                "inner".takeIf { isInner },
                "inline".takeIf { isInline },
                "data".takeIf { isData },
                keyword,
                nameIfExists,
                typeParametersForPrint
            ).joinToString(separator = " ")
        }
}


class DecompilerTreeInterface(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>,
    override val typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "interface"
    override val isDefaultModality: Boolean = element.modality == Modality.ABSTRACT

    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            listOfNotNull(
                computeModifiersAndName,
                primaryConstructor?.decompile(),
                superTypes.filterNot { it.irType.isAny() }.takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = ": ") { it.decompile() }
            ).joinToString(" ").also { print(it) }
            if (properties.isNotEmpty() || methods.isNotEmpty() || otherPrintableDeclarations.isNotEmpty()) {
                withBraces {
                    listOf(properties, methods, otherPrintableDeclarations).flatten().forEach {
                        it.produceSources(this)
                    }
                }
            }
        }
    }
}

class DecompilerTreeClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "class"
    private val isTrivialPrimaryCtorDelegate: Boolean
        get() = primaryConstructor?.delegatingConstructorCall?.type?.irType?.isAny() ?: false

    override fun produceSources(printer: SmartPrinter) {
        val nonTrivialCall = primaryConstructor?.delegatingConstructorCall
            ?.takeIf { !isTrivialPrimaryCtorDelegate }
        val nonTrivialCallDecompiled = nonTrivialCall?.let { "${it.type.decompile()}${it.decompile()}" }
        val nonTrivialSuperTypes = superTypes.filterNot { it.irType.isAny() }
            .filterNot { it.irType == nonTrivialCall?.type?.irType }

        with(printer) {
            listOfNotNull(
                computeModifiersAndName,
                primaryConstructor?.decompile(),
                ":".takeIf { nonTrivialCallDecompiled != null || nonTrivialSuperTypes.isNotEmpty() },
                nonTrivialCallDecompiled,
                nonTrivialSuperTypes.takeIf { it.isNotEmpty() }?.joinToString { it.decompile() }
            ).joinToString(" ").also { print(it) }

            withBraces {
                listOfNotNull(properties, initSections, secondaryConstructors, methods, otherPrintableDeclarations).flatten()
                    .forEach { it.produceSources(this) }
            }

        }
    }
}

class DecompilerTreeEnumClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "enum class"

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeAnnotationClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "annotation class"

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeObject(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "object"
    override val nameIfExists: String? = element.name().takeIf { it != "<no name provided>" }

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

internal fun buildClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeConstructorCall>,
    typeParameters: List<DecompilerTreeTypeParameter>,
    thisReceiver: DecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
): AbstractDecompilerTreeClass = when (element.kind) {
    INTERFACE -> DecompilerTreeInterface(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
    ANNOTATION_CLASS -> DecompilerTreeAnnotationClass(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
    ENUM_CLASS -> DecompilerTreeEnumClass(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
    //TODO is it enough for `object SomeObj` val x = object : Any {...}
    OBJECT -> DecompilerTreeObject(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
    else -> DecompilerTreeClass(element, declarations, annotations, typeParameters, thisReceiver, superTypes)
}

