/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeBody
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

interface DecompilerTreeFunction : DecompilerTreeDeclaration, SourceProducible {
    override val element: IrFunction
    val returnType: DecompilerTreeType
    var dispatchReceiverParameter: DecompilerTreeValueParameter?
    var extensionReceiverParameter: DecompilerTreeValueParameter?
    var valueParameters: List<DecompilerTreeValueParameter>
    var body: DecompilerTreeBody?
    override val annotationTarget: String?
        get() = null

    val modalityIfExists: String?
    open val isOverridden: Boolean
        get() = false
    open val isTailrec: Boolean
        get() = false
    open val isSuspend: Boolean
        get() = false
    open val isOperator: Boolean
        get() = false

    val functionFlags: List<String>
        get() = with(element) {
            listOfNotNull(
                visibilityIfExists?.takeIf { visibility != Visibilities.DEFAULT_VISIBILITY },
//                annotationSourcesList.joinToString(" ").takeIf { annotationSourcesList.isNotEmpty() },
//                visibilityIfExists?.takeIf { visibility != Visibilities.DEFAULT_VISIBILITY },
                // Could this be true for constructors
                "expect".takeIf { isExpect },
                modalityIfExists,
                "external".takeIf { isExternal },
                "override".takeIf { isOverridden },
                "tailrec".takeIf { isTailrec },
                "suspend".takeIf { isSuspend },
                "inline".takeIf { isInline },
                "operator".takeIf { isOperator }
            )
        }
    val valueParametersSources: List<String>
        get() = valueParameters.map { it.decompile() }

    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeSimpleFunction(
    override val element: IrSimpleFunction,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val returnType: DecompilerTreeType,
    override var dispatchReceiverParameter: DecompilerTreeValueParameter?,
    override var extensionReceiverParameter: DecompilerTreeValueParameter?,
    override var valueParameters: List<DecompilerTreeValueParameter>,
    override var body: DecompilerTreeBody?
) : DecompilerTreeFunction {

    //TODO implement workaround for interface open fun with default OPEN
    override val modalityIfExists: String? = element.modality.takeIf {
        it != Modality.FINAL
    }?.name?.toLowerCase()

    override val isOverridden: Boolean = element.overriddenSymbols.isNotEmpty()
            && element.overriddenSymbols.map { it.owner.name() }.contains(element.name())

    override val isTailrec: Boolean
        get() = element.isTailrec
    override val isSuspend: Boolean
        get() = element.isSuspend
    override val isOperator: Boolean
        get() = element.isOperator

}

class DecompilerTreeConstructor(
    override val element: IrConstructor,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val returnType: DecompilerTreeType,
    override var dispatchReceiverParameter: DecompilerTreeValueParameter?,
    override var extensionReceiverParameter: DecompilerTreeValueParameter?,
    override var valueParameters: List<DecompilerTreeValueParameter>,
    override var body: DecompilerTreeBody?
) : DecompilerTreeFunction {
    override val modalityIfExists: String? = null
//    override val keyword: String? =
//        "constructor".takeIf { annotations.isNotEmpty() || element.visibility != Visibilities.DEFAULT_VISIBILITY }
}