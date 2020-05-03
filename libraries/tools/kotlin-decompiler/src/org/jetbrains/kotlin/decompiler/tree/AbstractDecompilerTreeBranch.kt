/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeExpression
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrElseBranch

abstract class AbstractDecompilerTreeBranch(override val element: IrBranch) : DecompilerTreeElement, SourceProducible {
    abstract val condition: DecompilerTreeExpression
    abstract val result: DecompilerTreeExpression
}

class DecompilerTreeBranch(
    override val element: IrBranch,
    override val condition: DecompilerTreeExpression,
    override val result: DecompilerTreeExpression
) : AbstractDecompilerTreeBranch(element) {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

class DecompilerTreeElseBranch(
    override val element: IrElseBranch,
    override val condition: DecompilerTreeExpression,
    override val result: DecompilerTreeExpression
) : AbstractDecompilerTreeBranch(element) {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}