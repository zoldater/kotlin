/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.AbstractDecompilerTreeBranch
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.util.withBraces
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrWhen

abstract class AbstractDecompilerTreeWhen(
    override val element: IrWhen,
    val branches: List<AbstractDecompilerTreeBranch>,
    override val type: DecompilerTreeType
) : DecompilerTreeExpression, SourceProducible

//TODO extend WhenWithReceiver, WhenWithDeclaringReceiver
class DecompilerTreeWhen(
    element: IrWhen,
    branches: List<AbstractDecompilerTreeBranch>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeWhen(element, branches, type) {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            print("when")
            withBraces {
                branches.forEach { it.produceSources(this) }
            }
        }
    }
}

class DecompilerTreeIfThenElse(
    element: IrWhen, branches: List<AbstractDecompilerTreeBranch>,
    type: DecompilerTreeType
) : AbstractDecompilerTreeWhen(element, branches, type) {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}