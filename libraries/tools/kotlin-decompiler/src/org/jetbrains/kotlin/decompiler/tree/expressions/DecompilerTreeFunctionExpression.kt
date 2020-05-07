/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.indented
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeBlockBody
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.AbstractDecompilerTreeSimpleFunction
import org.jetbrains.kotlin.decompiler.tree.declarations.AbstractDecompilerTreeValueParameter
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeTypeParameter
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class DecompilerTreeFunctionExpression(
    override val element: IrFunctionExpression?,
    private val lambda: AbstractDecompilerTreeSimpleFunction,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression,
    SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        lambda.produceSources(printer)
    }
}

class DecompilerTreeLambdaFunction(
    override val element: IrSimpleFunction,
    override val returnType: DecompilerTreeType,
    override val dispatchReceiverParameter: AbstractDecompilerTreeValueParameter?,
    override val extensionReceiverParameter: AbstractDecompilerTreeValueParameter?,
    override val valueParameters: List<AbstractDecompilerTreeValueParameter>,
    override val body: DecompilerTreeBlockBody?
) : AbstractDecompilerTreeSimpleFunction {
    override val annotations: List<DecompilerTreeAnnotationConstructorCall> = emptyList()
    override val typeParameters: List<DecompilerTreeTypeParameter> = emptyList()
    override val annotationTarget: String? = null
    override val modalityIfExists: String? = null
    override var defaultVisibility: Visibility = Visibilities.LOCAL

    private val DecompilerTreeBlockBody.isSingleReturnBody: Boolean
        get() = statements.size == 1 && statements[0] is DecompilerTreeReturn

    val valueParameterNamesOrNull: String?
        get() = valueParameters.mapNotNull { it.nameIfExists }.ifNotEmpty { joinToString() }

    private fun SmartPrinter.printSingleReturnBody() {
        (body?.statements?.get(0) as? DecompilerTreeReturn)?.value?.also { retVal ->
            listOfNotNull(
                valueParameterNamesOrNull?.let { "$it ->" },
                retVal.decompile()
            ).joinToString(" ", prefix = "{ ", postfix = " }").also { print(it) }
        }
    }

    private fun SmartPrinter.printBodyAsUsual() {
        val valueParameters = valueParameterNamesOrNull?.let { "$it ->" }
        val statementsDecompiled = body?.statements?.map { it.decompile() }
        print("{ ")
        valueParameters?.also { println(it) } ?: println()
        indented {
            statementsDecompiled?.forEach { println(it) } ?: println("Unit")
        }
        println("}")
    }

    override fun produceSources(printer: SmartPrinter) {
        body?.takeIf { it.isSingleReturnBody }?.also { printer.printSingleReturnBody() } ?: printer.printBodyAsUsual()
    }
}