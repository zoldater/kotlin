/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations

import org.jetbrains.kotlin.decompiler.tree.expressions.call.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrProperty

class DecompilerTreeProperty(
    override val element: IrProperty,
    override val annotations: List<DecompilerTreeConstructorCall>
) : DecompilerTreeDeclaration