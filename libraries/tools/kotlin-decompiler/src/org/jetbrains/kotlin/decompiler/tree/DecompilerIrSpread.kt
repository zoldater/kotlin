/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.ir.expressions.IrSpreadElement

class DecompilerIrSpread(override val element: IrSpreadElement) : DecompilerIrElement<IrSpreadElement>, IrSpreadElement by element