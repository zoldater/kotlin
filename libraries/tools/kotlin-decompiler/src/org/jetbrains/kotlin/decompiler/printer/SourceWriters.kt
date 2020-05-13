/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.printer

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeFile
import org.jetbrains.kotlin.ir.declarations.path
import java.io.File

// почему у тебя этот и другие классы, которые являются entry point'ами
// в декомпилятор являются internal?
internal class FileSourcesWriter(val files: List<DecompilerTreeFile>) {
    val filesToContentMap: Map<DecompilerTreeFile, String>
        get() = files.map { it to it.decompile() }.toMap()


    fun writeFileSources(filePath: String): File {
        val rootFile = File(filePath)
        rootFile.mkdirs()

        filesToContentMap.forEach { (k, v) ->
            with(rootFile.resolve(k.element.path.removePrefix("/"))) {
                mkdirs()
                writeText(v)
            }
        }

        return rootFile
    }
}