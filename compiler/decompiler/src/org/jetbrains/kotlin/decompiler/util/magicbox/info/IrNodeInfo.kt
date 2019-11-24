/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util.magicbox.info

import org.jetbrains.kotlin.ir.IrElement

interface IrNodeInfo<T> {
    var calculatedListForImport: List<String>
    var calculatedName: String

    fun doResolveStep() {
        if (calculatedListForImport.size > 1) {
            calculatedListForImport = calculatedListForImport.dropLast(1)
            calculatedName = "${calculatedListForImport.last()}.$calculatedName"
        }
    }
}