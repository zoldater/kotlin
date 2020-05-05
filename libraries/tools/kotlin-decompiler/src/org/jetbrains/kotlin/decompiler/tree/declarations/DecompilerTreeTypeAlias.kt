/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeTypeParametersContainer
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias

class DecompilerTreeTypeAlias(
    override val element: IrTypeAlias,
    override val annotations: List<DecompilerTreeConstructorCall>,
    override val annotationTarget: String? = null,
    val aliasedType: DecompilerTreeType,
    override val typeParameters: List<DecompilerTreeTypeParameter>
) : DecompilerTreeDeclaration, DecompilerTreeTypeParametersContainer {
    override fun produceSources(printer: SmartPrinter) {
        with(element) {
            printer.println(
                listOfNotNull("actual".takeIf { isActual }, "typealias", name(), typeParametersForPrint, "=", aliasedType.decompile())
                    .joinToString(" ")
            )
        }
    }
}