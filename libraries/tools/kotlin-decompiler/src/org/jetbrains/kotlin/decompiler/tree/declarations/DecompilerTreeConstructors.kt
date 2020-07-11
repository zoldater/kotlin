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

    var isObjectConstructor: Boolean = false

    abstract val keyword: String?
    abstract val valueParametersOrNull: String?
    abstract val delegatingCallDecompiledOrNull: String?

    private val bodyStatementsNonTrivial: List<DecompilerTreeStatement>?
        get() = (body as? AbstractDecompilerTreeBlockBody)?.statements
            ?.filterNot { it is DecompilerTreeDelegatingConstructorCall || it is DecompilerTreeInstanceInitializerCall }

    protected val delegatingConstructorCall: DecompilerTreeDelegatingConstructorCall?
        get() = (body as? AbstractDecompilerTreeBlockBody)?.statements
            ?.filterIsInstance<DecompilerTreeDelegatingConstructorCall>()
            ?.firstOrNull()

    private val header: String?
        get() = listOfNotNull(
            annotationSourcesList.ifNotEmpty { joinToString(" ") },
            functionFlags.ifNotEmpty { joinToString(" ") },
            keyword
        ).ifNotEmpty { joinToString(" ") }

    open val headerWithPrimaryCtorOrNull: String?
        get() = listOfNotNull(header, valueParametersOrNull).ifNotEmpty { joinToString("") }

    override fun produceSources(printer: SmartPrinter) {

        listOfNotNull(headerWithPrimaryCtorOrNull, delegatingCallDecompiledOrNull?.let { ": $it" })
            .ifNotEmpty { joinToString(" ") }
            ?.let { if (headerWithPrimaryCtorOrNull == null) " $it" else it }
            ?.also { printer.print(it) }

        // Note. If primary constructor contains anything except DelegatingConstructorCall and InstanceInitializerCall
        // generated sources will be incorrect with inheritance/delegating records
        bodyStatementsNonTrivial?.ifNotEmpty {
            printer.withBraces {
                forEach {
                    it.decompileByLines(printer)
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

    override val headerWithPrimaryCtorOrNull: String?
        get() = super.headerWithPrimaryCtorOrNull?.takeIf { !isObjectConstructor }
    override val keyword: String? = "constructor".takeIf { !isTrivial }
    override val valueParametersOrNull: String?
        get() = valueParametersForPrint
    override val delegatingCallDecompiledOrNull: String?
        get() = delegatingConstructorCall?.takeIf { !it.isTrivial }
            ?.let { "${it.returnType.decompile()}${it.decompile()}" }
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
        get() = delegatingConstructorCall?.let { "${if (returnType == it.returnType) "this" else "super"}${it.decompile()}" }
}