/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.printer

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeFile
import org.jetbrains.kotlin.ir.declarations.path
import java.io.File

interface IFileSourcesWriter {
    val files: List<DecompilerTreeFile>
    fun filesToContentMap() = files.map { it to it.decompile() }.toMap()

    fun writeFileSources(filePath: String): File
}

class FileSourcesToSingleFileWriter(override val files: List<DecompilerTreeFile>) : IFileSourcesWriter {
    override fun writeFileSources(filePath: String): File {
        val fileContentsWithPathHeader = filesToContentMap()
            .map { (k, v) -> "// FILE: ${k.element.path}\n\n$v" }
            .joinToString("\n")

        val resultFile = File(filePath)
        resultFile.writeText(fileContentsWithPathHeader)
        return resultFile
    }
}

class FileSourcesToFileSystemWriter(override val files: List<DecompilerTreeFile>) : IFileSourcesWriter {
    override fun writeFileSources(filePath: String): File {
        val rootFile = File(filePath)
        rootFile.mkdirs()

        filesToContentMap().forEach { (k, v) ->
            with(rootFile.resolve(k.element.path.removePrefix("/"))) {
                mkdirs()
                writeText(v)
            }
        }

        return rootFile
    }
}