/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.containers

import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerIrDeclaration
import org.jetbrains.kotlin.decompiler.tree.expressions.call.DecompilerIrConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

interface DecompilerIrAnnotationsContainer {
    val annotations: List<DecompilerIrConstructorCall>
}