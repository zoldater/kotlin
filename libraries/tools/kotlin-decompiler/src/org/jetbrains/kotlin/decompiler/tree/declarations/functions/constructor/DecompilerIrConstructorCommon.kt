/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.functions.constructor

import org.jetbrains.kotlin.decompiler.tree.DecompilerIrElement
import org.jetbrains.kotlin.decompiler.tree.declarations.functions.DecompilerIrFunction
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrBranch

abstract class DecompilerIrConstructorCommon(override val element: IrConstructor) : DecompilerIrFunction