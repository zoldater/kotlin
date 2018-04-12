/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrFile

class FirSourceManager : SourceManager {

    class FirFileEntry(val file: FirFile) : SourceManager.FileEntry {
        override val name: String
            get() = file.name

        override val maxOffset: Int
            get() = TODO("not implemented")

        override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo {
            TODO("not implemented")
        }

        override fun getLineNumber(offset: Int): Int {
            TODO("not implemented")
        }

        override fun getColumnNumber(offset: Int): Int {
            TODO("not implemented")
        }

    }

    override fun getFileEntry(irFile: IrFile): SourceManager.FileEntry =
        fileEntriesByIrFile[irFile]!!

    private val fileEntriesByFirFile = mutableMapOf<FirFile, FirFileEntry>()
    private val fileEntriesByIrFile = mutableMapOf<IrFile, FirFileEntry>()

    private fun createFileEntry(file: FirFile): FirFileEntry {
        if (file in fileEntriesByFirFile) error("FirFileEntry is already created for $file")
        val newEntry = FirFileEntry(file)
        fileEntriesByFirFile[file] = newEntry
        //ktFileByFileEntry[newEntry] = ktFile
        return newEntry
    }

    fun putFileEntry(irFile: IrFile, fileEntry: FirFileEntry) {
        fileEntriesByIrFile[irFile] = fileEntry
    }

    fun getOrCreateFileEntry(file: FirFile): FirFileEntry =
        fileEntriesByFirFile.getOrElse(file) { createFileEntry(file) }

}