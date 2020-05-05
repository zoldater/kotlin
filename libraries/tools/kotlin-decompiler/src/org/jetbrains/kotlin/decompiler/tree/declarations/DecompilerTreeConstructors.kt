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
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
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
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val returnType: DecompilerTreeType,
    override var dispatchReceiverParameter: DecompilerTreeValueParameter?,
    override var extensionReceiverParameter: DecompilerTreeValueParameter?,
    override var valueParameters: List<DecompilerTreeValueParameter>,
    override var body: DecompilerTreeBody?,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override var defaultVisibility: Visibility = Visibilities.PUBLIC
) : DecompilerTreeFunction {
    override val modalityIfExists: String? = null
    val isTrivial: Boolean
        get() = annotationSourcesList.isEmpty() && functionFlags.isEmpty()

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
                forEach { it.produceSources(printer) }
            }
        }

    }
}

class DecompilerTreePrimaryConstructor(
    element: IrConstructor,
    annotations: List<DecompilerTreeConstructorCall>,
    returnType: DecompilerTreeType,
    dispatchReceiverParameter: DecompilerTreeValueParameter?,
    extensionReceiverParameter: DecompilerTreeValueParameter?,
    valueParameters: List<DecompilerTreeValueParameter>,
    body: DecompilerTreeBody?,
    typeParameters: List<DecompilerTreeTypeParameter>
//TODO Place for map superType to delegate
) : AbstractDecompilerTreeConstructor(
    element, annotations, returnType, dispatchReceiverParameter, extensionReceiverParameter, valueParameters, body, typeParameters,
) {
    private val DecompilerTreeDelegatingConstructorCall.isTrivial: Boolean
        get() = element.type.isAny() || element.type.isUnit()

    override val keyword: String? = "constructor".takeIf { !isTrivial }
    override val valueParametersOrNull: String? = valueParametersForPrint.takeIf { valueParameters.isNotEmpty() }
    override val delegatingCallDecompiledOrNull: String?
        get() = delegatingConstructorCall?.let { "${it.type.decompile()}${it.decompile()}" }
            ?.takeIf { !delegatingConstructorCall!!.isTrivial }
}

class DecompilerTreeSecondaryConstructor(
    element: IrConstructor,
    annotations: List<DecompilerTreeConstructorCall>,
    returnType: DecompilerTreeType,
    dispatchReceiverParameter: DecompilerTreeValueParameter?,
    extensionReceiverParameter: DecompilerTreeValueParameter?,
    valueParameters: List<DecompilerTreeValueParameter>,
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