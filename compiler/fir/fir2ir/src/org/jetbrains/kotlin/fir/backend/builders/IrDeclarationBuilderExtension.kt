/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.builders

import org.jetbrains.kotlin.fir.backend.FirBasedIrBuilderContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.withScope

abstract class IrDeclarationBuilderExtension(val builder: IrDeclarationBuilder) {
    val context: FirBasedIrBuilderContext get() = builder.context

    val container get() = builder.container

    inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            context.symbolTable.withScope(irDeclaration.descriptor) {
                builder(irDeclaration)
            }
        }
}