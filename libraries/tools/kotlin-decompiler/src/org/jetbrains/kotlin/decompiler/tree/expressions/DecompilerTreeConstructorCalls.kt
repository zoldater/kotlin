/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

interface AbstractDecompilerTreeConstructorCall : DecompilerTreeMemberAccessExpression, SourceProducible {

    override val element: IrConstructorCall
    override val dispatchReceiver: DecompilerTreeExpression?
    override val extensionReceiver: DecompilerTreeExpression?
    override val valueArguments: List<DecompilerTreeExpression>
    override val type: DecompilerTreeType
    override val typeArguments: List<DecompilerTreeType>

    val valueArgumentsDecompiled: String?

    override fun produceSources(printer: SmartPrinter) {
        listOfNotNull(
            dispatchReceiver?.decompile()?.let { "$it.${type.decompile()}" }
                ?: extensionReceiver?.decompile()?.let { "$it.${type.decompile()}" }
                ?: type.decompile(),
            typeArgumentsForPrint,
            valueArgumentsDecompiled
        ).joinToString("").also {
            printer.print(it)
        }
    }
}

class DecompilerTreeCommonConstructorCall(
    override val element: IrConstructorCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>
) : AbstractDecompilerTreeConstructorCall {
    override val valueArgumentsDecompiled: String = valueArgumentsInsideParenthesesOrNull ?: "()"
}

class DecompilerTreeAnnotationConstructorCall(
    override val element: IrConstructorCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>
) : AbstractDecompilerTreeConstructorCall {
    override val valueArgumentsDecompiled: String? = valueArgumentsInsideParenthesesOrNull
}

class DecompilerTreeDelegatingConstructorCall(
    override val element: IrDelegatingConstructorCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType,
    override val typeArguments: List<DecompilerTreeType>,
    internal var returnType: DecompilerTreeType
) : DecompilerTreeMemberAccessExpression, SourceProducible {

    override fun produceSources(printer: SmartPrinter) {
        listOfNotNull(
            typeArgumentsForPrint,
            valueArgumentsInsideParenthesesOrNull ?: "()"
        ).ifNotEmpty {
            joinToString("").also {
                printer.print(it)
            }
        }
    }
}

class DecompilerTreeEnumConstructorCall(
    override val element: IrEnumConstructorCall,
    override val dispatchReceiver: DecompilerTreeExpression?,
    override val extensionReceiver: DecompilerTreeExpression?,
    override val valueArguments: List<DecompilerTreeExpression>,
    override val type: DecompilerTreeType
) : DecompilerTreeMemberAccessExpression, SourceProducible {

    override val typeArguments: List<DecompilerTreeType> = emptyList()

    override fun produceSources(printer: SmartPrinter) {
        valueArgumentsInsideParenthesesOrNull?.also { printer.print(it) }
    }

}