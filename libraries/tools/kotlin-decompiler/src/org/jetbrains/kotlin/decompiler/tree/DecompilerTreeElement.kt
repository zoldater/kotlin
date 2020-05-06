/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.printer.SourceProducible
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement

interface DecompilerTreeElement {
    val element: IrElement?
}

interface DecompilerTreeVarargElement : DecompilerTreeElement, SourceProducible

interface DecompilerTreeStatement : DecompilerTreeElement, SourceProducible {
    override val element: IrStatement?
}