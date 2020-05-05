/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.printer

import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter

internal inline fun SmartPrinter.indented(body: () -> Unit) {
    pushIndent()
    body()
    popIndent()
}

internal inline fun SmartPrinter.withBraces(body: () -> Unit) {
    println(" {")
    indented(body)
    println("} ")
}

internal inline fun SmartPrinter.insideParentheses(body: () -> Unit) {
    print("(")
    body()
    print(")")
}



interface SourceProducible {
    fun produceSources(printer: SmartPrinter)

    //TODO Maybe will need to split lines and decrease indent
    fun decompile() = StringBuilder().also { sb -> produceSources(SmartPrinter(sb)) }.toString()
}