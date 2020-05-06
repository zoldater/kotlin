/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.AbstractDecompilerTreeBlockBody
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeBody
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeStatement
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeDelegatingConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeInstanceInitializerCall
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty


abstract class AbstractDecompilerTreeConstructor(
    override val element: IrConstructor,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override val returnType: DecompilerTreeType,
    override var dispatchReceiverParameter: AbstractDecompilerTreeValueParameter?,
    override var extensionReceiverParameter: AbstractDecompilerTreeValueParameter?,
    override var valueParameters: List<AbstractDecompilerTreeValueParameter>,
    override var body: DecompilerTreeBody?,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override var defaultVisibility: Visibility = Visibilities.PUBLIC
) : DecompilerTreeFunction {
    override val modalityIfExists: String? = null
    val isTrivial: Boolean
        get() = annotationSourcesList.isEmpty() && functionFlags.isEmpty() && element.visibility == defaultVisibility

    abstract val keyword: String?
    abstract val valueParametersOrNull: String?
    abstract val delegatingCallDecompiledOrNull: String?

    private val bodyStatementsNonTrivial: List<DecompilerTreeStatement>?
        get() = (body as? AbstractDecompilerTreeBlockBody)?.statements
            ?.filterNot { it is DecompilerTreeDelegatingConstructorCall || it is DecompilerTreeInstanceInitializerCall }

    val delegatingConstructorCall: DecompilerTreeDelegatingConstructorCall?
        get() = (body as? AbstractDecompilerTreeBlockBody)?.statements
            ?.filterIsInstance(DecompilerTreeDelegatingConstructorCall::class.java)?.firstOrNull()

    override fun produceSources(printer: SmartPrinter) {
        val header = listOfNotNull(
            annotationSourcesList.ifNotEmpty { joinToString(" ") },
            functionFlags.ifNotEmpty { joinToString(" ") },
            keyword
        ).ifNotEmpty { joinToString(" ") }

        val headerWithPrimaryCtorOrNull = listOfNotNull(header, valueParametersOrNull).ifNotEmpty { joinToString("") }


        listOfNotNull(headerWithPrimaryCtorOrNull, delegatingCallDecompiledOrNull?.let { ": $it" })
            .ifNotEmpty { joinToString(" ") }
            ?.also { printer.print(it) }

        // Note. If primary constructor contains anything except DelegatingConstructorCall and InstanceInitializerCall
        // generated sources will be incorrect with inheritance/delegating records
        bodyStatementsNonTrivial?.ifNotEmpty {
            printer.withBraces {
                joinToString("\n") { it.decompile() }.lines().forEach {
                    printer.println(it)
                }
            }
        }

    }
}

class DecompilerTreePrimaryConstructor(
    element: IrConstructor,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    returnType: DecompilerTreeType,
    dispatchReceiverParameter: AbstractDecompilerTreeValueParameter?,
    extensionReceiverParameter: AbstractDecompilerTreeValueParameter?,
    valueParameters: List<AbstractDecompilerTreeValueParameter>,
    body: DecompilerTreeBody?,
    typeParameters: List<DecompilerTreeTypeParameter>
//TODO Place for map superType to delegate
) : AbstractDecompilerTreeConstructor(
    element, annotations, returnType, dispatchReceiverParameter, extensionReceiverParameter, valueParameters, body, typeParameters,
) {
    private val DecompilerTreeDelegatingConstructorCall.isTrivial: Boolean
        get() = returnType.irType.isAny() || returnType.irType.isUnit()

    override val keyword: String? = "constructor".takeIf { !isTrivial }
    override val valueParametersOrNull: String?
        get() = valueParametersForPrint
    override val delegatingCallDecompiledOrNull: String?
        get() = delegatingConstructorCall?.takeIf { !it.isTrivial }
            ?.let { "${it.returnType.decompile()}${it.decompile()}" }
}

class DecompilerTreeDataClassPrimaryConstructor(
    element: IrConstructor,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    returnType: DecompilerTreeType,
    dispatchReceiverParameter: AbstractDecompilerTreeValueParameter?,
    extensionReceiverParameter: AbstractDecompilerTreeValueParameter?,
    override var valueParameters: List<AbstractDecompilerTreeValueParameter>,
    body: DecompilerTreeBody?,
    typeParameters: List<DecompilerTreeTypeParameter>
//TODO Place for map superType to delegate
) : AbstractDecompilerTreeConstructor(
    element, annotations, returnType, dispatchReceiverParameter, extensionReceiverParameter, valueParameters, body, typeParameters,
) {
    private val DecompilerTreeDelegatingConstructorCall.isTrivial: Boolean
        get() = returnType.irType.isAny() || returnType.irType.isUnit()

    override val keyword: String? = null
    override val valueParametersOrNull: String?
        get() = valueParametersForPrint
    override val delegatingCallDecompiledOrNull: String? = null

    override fun produceSources(printer: SmartPrinter) {
        super.produceSources(printer)
    }
}

class DecompilerTreeSecondaryConstructor(
    element: IrConstructor,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    returnType: DecompilerTreeType,
    dispatchReceiverParameter: AbstractDecompilerTreeValueParameter?,
    extensionReceiverParameter: AbstractDecompilerTreeValueParameter?,
    valueParameters: List<AbstractDecompilerTreeValueParameter>,
    body: DecompilerTreeBody?,
    typeParameters: List<DecompilerTreeTypeParameter>,
) : AbstractDecompilerTreeConstructor(
    element, annotations, returnType, dispatchReceiverParameter, extensionReceiverParameter, valueParameters, body, typeParameters,
) {
    override val keyword: String = "constructor"
    override val valueParametersOrNull: String = valueParametersForPrint
    override val delegatingCallDecompiledOrNull: String?
        get() = delegatingConstructorCall?.let { "this${it.decompile()}" }

}