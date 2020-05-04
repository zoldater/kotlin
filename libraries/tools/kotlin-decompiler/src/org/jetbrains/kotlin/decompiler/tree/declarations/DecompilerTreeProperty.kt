/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.ir.declarations.IrProperty

class DecompilerTreeProperty(
    override val element: IrProperty,
    override val annotations: List<DecompilerTreeConstructorCall>,
    private val backingField: DecompilerTreeField?,
    var getter: DecompilerTreeSimpleFunction?,
    var setter: DecompilerTreeSimpleFunction?
) : DecompilerTreeDeclaration {
    override val annotationTarget: String = "property"

    override fun produceSources(printer: SmartPrinter) {
        with(element) {
            listOfNotNull(
                visibilityIfExists,
                "expect".takeIf { isExpect },
                modality.takeIf { it != Modality.FINAL }?.name,
                "external".takeIf { isExternal },
//                "external".takeIf { isFakeOverride },


                "const".takeIf { isConst })
        }
        TODO("Not yet implemented")
    }

}