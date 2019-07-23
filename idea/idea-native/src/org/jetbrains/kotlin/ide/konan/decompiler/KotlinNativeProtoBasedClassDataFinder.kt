/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.konan.KonanMetadataVersion

class KotlinNativeProtoBasedClassDataFinder(
    proto: KonanProtoBuf.LinkDataPackageFragment,
    private val nameResolver: NameResolver,
    private val classSource: (ClassId) -> SourceElement = { SourceElement.NO_SOURCE }
) : ClassDataFinder {
    private val classIdToProto =
        proto.classes.classesList.associateBy { klass ->
            nameResolver.getClassId(klass.fqName)
        }

    override fun findClassData(classId: ClassId): ClassData? {
        val classProto = classIdToProto[classId] ?: return null
        return ClassData(nameResolver, classProto, KonanMetadataVersion.INSTANCE, classSource(classId))
    }
}
