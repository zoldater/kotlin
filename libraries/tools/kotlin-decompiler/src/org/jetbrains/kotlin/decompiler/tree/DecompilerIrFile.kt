/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.tree.containers.DecompilerIrDeclarationContainer
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerIrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile

class DecompilerIrFile(
    override val element: IrFile,
    override val declarations: List<DecompilerIrDeclaration>
) : DecompilerIrElement<IrFile>, DecompilerIrDeclarationContainer