/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.branch

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeElement
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeExpression
import org.jetbrains.kotlin.ir.expressions.IrBranch

abstract class DecompilerTreeBranch(override val element: IrBranch) : DecompilerTreeElement {
    abstract val condition: DecompilerTreeExpression
    abstract val result: DecompilerTreeExpression
}
