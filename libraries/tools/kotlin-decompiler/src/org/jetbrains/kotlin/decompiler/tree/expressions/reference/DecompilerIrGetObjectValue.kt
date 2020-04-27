/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions.reference

import org.jetbrains.kotlin.decompiler.printer.DecompilerIrSourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerIrStatement
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerIrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.utils.Printer

class DecompilerIrGetObjectValue(override val element: IrGetObjectValue) : DecompilerIrExpression, DecompilerIrSourceProducible {
    override fun produceSources(printer: Printer) {
        //TODO replace fqName with short name
        //TODO try to find out NPE cases
        printer.print(element.symbol.owner.fqNameWhenAvailable!!.asString())
    }
}