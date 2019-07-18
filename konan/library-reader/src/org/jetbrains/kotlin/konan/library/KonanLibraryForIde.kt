/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.library.impl.*
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.konan.parseModuleHeader
import org.jetbrains.kotlin.serialization.konan.parsePackageFragment

private const val KONAN_KLIB_PROPERTY_INTEROP = "interop"
private const val KONAN_KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS = "exportForwardDeclarations"

val BaseKotlinLibrary.isInterop
    get() = manifestProperties.getProperty(KONAN_KLIB_PROPERTY_INTEROP) == "true"

val BaseKotlinLibrary.exportForwardDeclarations
    get() = manifestProperties.getProperty(KONAN_KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS)
        .split(' ').asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { FqName(it) }
        .toList()

open class KonanLibraryMetadataLoader {
    open fun loadModuleHeader(
        library: KonanLibraryForIde
    ): KonanProtoBuf.LinkDataLibrary = parseModuleHeader(library.moduleHeaderData)

    open fun loadPackageFragment(
        library: KonanLibraryForIde,
        packageFqName: String,
        partName: String
    ): KonanProtoBuf.LinkDataPackageFragment = parsePackageFragment(library.packageMetadata(packageFqName, partName))
}

class KonanLibraryForIde(
    base: BaseKotlinLibrary,
    private val metadata: MetadataLibraryImpl
) : BaseKotlinLibrary by base,
    MetadataLibrary by metadata {

    val metadataLayout: MetadataLibraryLayoutImpl
        get() = metadata.access.layout
}

fun createKonanLibraryForIde(
    libraryFile: KFile
): KonanLibraryForIde {
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile)
    val metadataAccess = MetadataLibraryAccess<MetadataKotlinLibraryLayout>(libraryFile)

    val base = BaseKotlinLibraryImpl(baseAccess, false)
    val metadata = MetadataLibraryImpl(metadataAccess)

    return KonanLibraryForIde(base, metadata)
}
