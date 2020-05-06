/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.AbstractDecompilerTreeExpressionBody
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeExpression
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable

interface DecompilerTreeValueDeclaration : DecompilerTreeDeclaration {
    override val element: IrValueDeclaration
    val type: DecompilerTreeType
}

abstract class AbstractDecompilerTreeVariable(
    override val element: IrVariable,
    override val annotations: List<DecompilerTreeAnnotationConstructorCall>,
    val initializer: DecompilerTreeExpression?,
    override val annotationTarget: String? = null
) : DecompilerTreeValueDeclaration

class DecompilerTreeVariable(
    element: IrVariable,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    initializer: DecompilerTreeExpression?,
    override val type: DecompilerTreeType
) : AbstractDecompilerTreeVariable(element, annotations, initializer) {
    override fun produceSources(printer: SmartPrinter) {
        with(element) {
            listOfNotNull("const".takeIf { isConst },
                          "lateinit".takeIf { isLateinit },
                          "var".takeIf { isVar } ?: "val",
                          nameIfExists, //TODO now we do not print types for variables
                          this@DecompilerTreeVariable.initializer?.let { "= ${it.decompile()}" }
                //TODO maybe paste run {...} for some initializers like = when(val t = smth()) {...}
            ).joinToString(" ").also { printer.println(it) }
        }
    }
}

class DecompilerTreeCatchParameterVariable(
    element: IrVariable,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override val type: DecompilerTreeType,
) : AbstractDecompilerTreeVariable(element, annotations, null) {
    override fun produceSources(printer: SmartPrinter) {
        printer.print("${element.name()}: ${type.decompile()}")
    }
}