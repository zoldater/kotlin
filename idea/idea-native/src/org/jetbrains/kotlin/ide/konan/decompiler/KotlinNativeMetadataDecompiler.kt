/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.konan.KonanMetadataVersion
import org.jetbrains.kotlin.serialization.konan.KonanSerializerProtocol
import org.jetbrains.kotlin.serialization.konan.NullFlexibleTypeDeserializer
import javax.swing.Icon

class KotlinNativeMetadataDecompiler : KotlinNativeMetadataDecompilerBase<KonanMetadataVersion>(
    KotlinNativeMetaFileType, { KonanSerializerProtocol }, NullFlexibleTypeDeserializer,
    { KonanMetadataVersion.INSTANCE },
    { KonanMetadataVersion.INVALID_VERSION },
    KotlinNativeMetaFileType.STUB_VERSION
) {

    override fun doReadFile(file: VirtualFile): FileWithMetadata? {
        val proto = KotlinNativeLoadingMetadataCache.getInstance().getCachedPackageFragment(file)
        return FileWithMetadata.Compatible(proto, KonanSerializerProtocol) //todo: check version compatibility
    }
}

object KotlinNativeMetaFileType : FileType {
    override fun getName() = "KNM"
    override fun getDescription() = "Kotlin/Native Metadata"
    override fun getDefaultExtension() = KLIB_METADATA_FILE_EXTENSION
    override fun getIcon(): Icon? = null
    override fun isBinary() = true
    override fun isReadOnly() = true
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    const val STUB_VERSION = 2
}

class KotlinNativeMetaFileTypeFactory : FileTypeFactory() {

    override fun createFileTypes(consumer: FileTypeConsumer) = consumer.consume(KotlinNativeMetaFileType, KotlinNativeMetaFileType.defaultExtension)
}
