/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeDelegatingConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeInstanceInitializerCall
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

interface DecompilerTreeFunction : DecompilerTreeDeclaration, DecompilerTreeTypeParametersContainer, SourceProducible {
    override val element: IrFunction
    val returnType: DecompilerTreeType
    var dispatchReceiverParameter: DecompilerTreeValueParameter?
    var extensionReceiverParameter: DecompilerTreeValueParameter?
    var valueParameters: List<DecompilerTreeValueParameter>
    var body: DecompilerTreeBody?
    override val annotationTarget: String?
        get() = null

    val modalityIfExists: String?
    var defaultVisibility: Visibility

    val isOverridden: Boolean
        get() = false
    val isTailrec: Boolean
        get() = false
    val isSuspend: Boolean
        get() = false
    val isOperator: Boolean
        get() = false

    val functionFlags: List<String>
        get() = with(element) {
            listOfNotNull(
                visibilityIfExists?.takeIf { visibility != defaultVisibility },
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
    val valueParametersForPrint: String
        get() = valueParameters.joinToString(", ", prefix = "(", postfix = ")") { it.decompile() }
}

open class DecompilerTreeSimpleFunction(
    override val element: IrSimpleFunction,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val returnType: DecompilerTreeType,
    override var dispatchReceiverParameter: DecompilerTreeValueParameter?,
    override var extensionReceiverParameter: DecompilerTreeValueParameter?,
    override var valueParameters: List<DecompilerTreeValueParameter>,
    override var body: DecompilerTreeBody?,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    var defaultModality: Modality = Modality.FINAL,
    override var defaultVisibility: Visibility = Visibilities.PUBLIC
) : DecompilerTreeFunction {

    //TODO implement workaround for interface open fun with default ABSTRACT
    override val modalityIfExists: String?
        get() = element.modality.takeIf { it != defaultModality }?.name?.toLowerCase()

    override val isOverridden: Boolean = element.overriddenSymbols.isNotEmpty()
            && element.overriddenSymbols.map { it.owner.name() }.contains(element.name())

    override val isTailrec: Boolean
        get() = element.isTailrec
    override val isSuspend: Boolean
        get() = element.isSuspend
    override val isOperator: Boolean
        get() = element.isOperator

    open override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            listOfNotNull(
                listOfNotNull(functionFlags.ifNotEmpty { joinToString(" ") }, "fun", nameIfExists)
                    .ifNotEmpty { joinToString(" ") },
                typeParametersForPrint, valueParametersForPrint
            ).joinToString("").also { print(it) }

            body?.also {
                withBraces {
                    it.produceSources(this)
                }
            } ?: println()
        }
    }
}

class DecompilerTreeCustomGetter(
    element: IrSimpleFunction,
    annotations: List<DecompilerTreeConstructorCall>,
    returnType: DecompilerTreeType,
    dispatchReceiverParameter: DecompilerTreeValueParameter?,
    extensionReceiverParameter: DecompilerTreeValueParameter?,
    valueParameters: List<DecompilerTreeValueParameter>,
    body: DecompilerTreeBody?,
    typeParameters: List<DecompilerTreeTypeParameter>,
    defaultModality: Modality = Modality.FINAL,
    defaultVisibility: Visibility = Visibilities.PUBLIC
) : DecompilerTreeSimpleFunction(
    element,
    annotations,
    returnType,
    dispatchReceiverParameter,
    extensionReceiverParameter,
    valueParameters,
    body,
    typeParameters,
    defaultModality,
    defaultVisibility
) {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print("get()")
            withBraces {
                body?.produceSources(this)
            }
        }
    }
}

class DecompilerTreeCustomSetter(
    element: IrSimpleFunction,
    annotations: List<DecompilerTreeConstructorCall>,
    returnType: DecompilerTreeType,
    dispatchReceiverParameter: DecompilerTreeValueParameter?,
    extensionReceiverParameter: DecompilerTreeValueParameter?,
    valueParameters: List<DecompilerTreeValueParameter>,
    body: DecompilerTreeBody?,
    typeParameters: List<DecompilerTreeTypeParameter>,
    defaultModality: Modality = Modality.FINAL,
    defaultVisibility: Visibility = Visibilities.PUBLIC
) : DecompilerTreeSimpleFunction(
    element,
    annotations,
    returnType,
    dispatchReceiverParameter,
    extensionReceiverParameter,
    valueParameters,
    body,
    typeParameters,
    defaultModality,
    defaultVisibility
) {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print("set(value)")
            withBraces {
                body?.produceSources(this)
            }
        }
    }
}
