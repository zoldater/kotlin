/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.expressions

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

class DecompilerTreeGetEnumValue(override val element: IrGetEnumValue) : DecompilerTreeExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        //TODO replace fqName with short name
        //TODO try to find out NPE cases
        printer.print(element.symbol.owner.fqNameWhenAvailable!!.asString())
    }
}

class DecompilerTreeGetObjectValue(override val element: IrGetObjectValue) : DecompilerTreeExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        //TODO replace fqName with short name
        //TODO try to find out NPE cases
        printer.print(element.symbol.owner.fqNameWhenAvailable!!.asString())
    }
}

class DecompilerTreeGetValue(override val element: IrGetValue) : DecompilerTreeExpression, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}