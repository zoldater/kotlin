/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// scout -- очень плохое название пакета. По нему вообще не понятно, что тут происходит
package org.jetbrains.kotlin.decompiler.scout

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeElement

// то же самое
interface IDecompilerTreeElementScout {
    fun scout(element: DecompilerTreeElement)
}