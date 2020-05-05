/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeDeclaration
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.name.FqName

class DecompilerTreeFile(
    override val element: IrFile,
    override val declarations: List<DecompilerTreeDeclaration>,
    override val annotations: List<DecompilerTreeConstructorCall>
) : DecompilerTreeElement, DecompilerTreeDeclarationContainer, DecompilerTreeAnnotationsContainer, SourceProducible {
    override fun produceSources(printer: SmartPrinter) {
        with(printer) {
            annotationSourcesList.forEach { println(it) }
            println()
            element.fqName.takeIf { it != FqName.ROOT }?.also { println("package $it", "\n") }
            declarations.joinToString("\n") { it.decompile() }.also {
                println(it)
            }
        }
    }

    override val annotationTarget: String = "file"
}