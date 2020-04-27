/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.branch

import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerIrExpression
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression

class DecompilerIrBranchRegular(
    override val element: IrBranch,
    override val dirCondition: DecompilerIrExpression,
    override val dirResult: DecompilerIrExpression
) : DecompilerIrBranch(element)