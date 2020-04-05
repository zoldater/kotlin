/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.printer.declarations

import org.jetbrains.kotlin.decompiler.printer.AbstractIrElementPrinter
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrClass

abstract class AbstractIrClassDeclarationPrinter(val irClass: IrClass) :
    AbstractIrElementPrinter<IrClass> {
    val visibilityStr: String?
        get() = irClass.visibility.name.takeIf { irClass.visibility != Visibilities.PUBLIC }

    val expectActualStr: String? by lazy {
        when {
            irClass.isExpect -> "expect"
            irClass.descriptor.isActual -> "actual"
            else -> null
        }
    }

    val modalityStr: String?
        get() = irClass.modality.name.takeIf { irClass.modality != Modality.FINAL }

    val externalStr: String?
        get() = "external".takeIf { irClass.isExternal }

    val innerStr: String?
        get() = "inner".takeIf { irClass.isInner }

    //    enum / enum entry / annotation / object / companion / data
    abstract val classKindStr: String

    val inlineStr: String?
        get() = "inline".takeIf { irClass.isInline }

//    val typeParametersStr: String? by lazy {
//        irClass.typeParameters.map {
//
//        }
//    }
//


}
