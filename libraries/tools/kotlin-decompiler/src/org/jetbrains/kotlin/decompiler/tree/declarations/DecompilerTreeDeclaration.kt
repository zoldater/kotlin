/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeAnnotationsContainer
import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility

fun IrDeclaration.name(): String = descriptor.name.asString()

interface DecompilerTreeDeclaration : DecompilerTreeStatement, DecompilerTreeAnnotationsContainer, SourceProducible {
    override val element: IrDeclaration

    val nameIfExists: String?
        get() = (element as? IrDeclarationWithName)?.name?.asString()

    val visibilityIfExists: String?
        get() = (element as? IrDeclarationWithVisibility)?.visibility?.name

}