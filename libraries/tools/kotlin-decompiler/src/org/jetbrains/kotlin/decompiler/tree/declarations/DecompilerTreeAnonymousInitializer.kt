/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.printer.withBraces
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeBlockBody
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer

class DecompilerTreeAnonymousInitializer(
    override val element: IrAnonymousInitializer,
    private val body: DecompilerTreeBlockBody,
    override val annotations: List<DecompilerTreeConstructorCall> = emptyList(),
    override val annotationTarget: String? = null
) : DecompilerTreeDeclaration, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            //TODO investigate isStatic flag effect
            print("init")
            withBraces {
                body.produceSources(this)
            }
        }
    }
}