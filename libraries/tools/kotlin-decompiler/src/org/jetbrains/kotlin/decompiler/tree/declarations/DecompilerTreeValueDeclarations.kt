/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeExpressionBody
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeExpression
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable

interface DecompilerTreeValueDeclaration : DecompilerTreeDeclaration {
    val type: DecompilerTreeType
}

class DecompilerTreeValueParameter(
    override val element: IrValueParameter,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val annotationTarget: String?,
    override val type: DecompilerTreeType,
    val varargType: DecompilerTreeType? = null,
    var defaultValue: DecompilerTreeExpressionBody? = null
) : DecompilerTreeValueDeclaration {
    override fun produceSources(printer: SmartPrinter) {
        TODO("Not yet implemented")
    }
}

abstract class AbstractDecompilerTreeVariable(
    override val element: IrVariable,
    override val annotations: List<DecompilerTreeConstructorCall>,
    val initializer: DecompilerTreeExpression?,
    override val annotationTarget: String? = null
) : DecompilerTreeValueDeclaration

class DecompilerTreeVariable(
    override val element: IrVariable,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val type: DecompilerTreeType,
    initializer: DecompilerTreeExpression?
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
    override val element: IrVariable,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val type: DecompilerTreeType,
) : AbstractDecompilerTreeVariable(element, annotations, null) {
    override fun produceSources(printer: SmartPrinter) {
        printer.print("${element.name()}: ${type.decompile()}")
    }
}