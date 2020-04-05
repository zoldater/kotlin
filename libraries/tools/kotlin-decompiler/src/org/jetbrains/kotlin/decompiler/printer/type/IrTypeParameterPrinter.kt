/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.printer.type

import org.jetbrains.kotlin.decompiler.printer.AbstractIrElementPrinter
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.types.Variance.IN_VARIANCE
import org.jetbrains.kotlin.types.Variance.OUT_VARIANCE

class IrTypeParameterPrinter(val irTypeParameter: IrTypeParameter) :
    AbstractIrElementPrinter<IrTypeParameter> {
    val reifiedStr: String?
        get() = "reified".takeIf { irTypeParameter.isReified }

    val varianceStr: String? = when (irTypeParameter.variance) {
        IN_VARIANCE -> "in"
        OUT_VARIANCE -> "out"
        else -> null
    }

    val supertypesStr: String?
        // Investigate annotations here
        get() = irTypeParameter.superTypes.joinToString { it.obtainTypeDescription() }


    override fun printTo(out: Appendable) {
    }
}