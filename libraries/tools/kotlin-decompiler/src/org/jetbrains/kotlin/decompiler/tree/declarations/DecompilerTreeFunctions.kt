/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

interface DecompilerTreeFunction : DecompilerTreeDeclaration, SourceProducible

class DecompilerTreeSimpleFunction(
    override val element: IrSimpleFunction,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val annotationTarget: String? = null
) : DecompilerTreeFunction {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

abstract class DecompilerTreeConstructorCommon(
    override val element: IrConstructor,
    override val annotations: List<DecompilerTreeConstructorCall>
) : DecompilerTreeFunction

class DecompilerTreeConstructorPrimary(
    override val element: IrConstructor,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val annotationTarget: String? = null
) : DecompilerTreeConstructorCommon(element, annotations) {
    override fun produceSources(printer: SmartPrinter) {
        val annotations = annotationSourcesList.joinToString(" ").takeIf { annotationSourcesList.isNotEmpty() }
        val visibility = visibilityIfExists?.takeIf { element.visibility != Visibilities.DEFAULT_VISIBILITY }

    }
}

class DecompilerTreeConstructorSecondary(
    override val element: IrConstructor,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val annotationTarget: String? = null
) : DecompilerTreeConstructorCommon(element, annotations) {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}