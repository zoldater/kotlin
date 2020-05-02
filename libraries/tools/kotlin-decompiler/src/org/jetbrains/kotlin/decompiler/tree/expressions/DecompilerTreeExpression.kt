/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeStatement
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeStatementsContainer
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression

interface DecompilerTreeExpression : DecompilerTreeStatement, SourceProducible {
    override val element: IrExpression
}

interface DecompilerTreeMemberAccessExpression : DecompilerTreeExpression {
    val valueArguments: List<DecompilerTreeExpression>
    val dispatchReceiver: DecompilerTreeExpression?
    val extensionReceiver: DecompilerTreeExpression?

    val valueArgumentsInsideParentheses: String
        get() = valueArguments.joinToString(", ", "(", ")") { it.decompile() }
            .takeIf { valueArguments.isNotEmpty() } ?: ""

}

class DecompilerTreeContainerExpression(
    override val element: IrContainerExpression,
    override val declarations: List<DecompilerTreeStatement>
) : DecompilerTreeExpression, DecompilerTreeStatementsContainer {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}