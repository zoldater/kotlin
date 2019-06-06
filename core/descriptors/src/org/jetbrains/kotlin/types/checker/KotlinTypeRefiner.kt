/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.refinement.TypeRefinement

abstract class KotlinTypeRefiner {
    @TypeRefinement
    abstract fun refineType(type: KotlinType): KotlinType

    @TypeRefinement
    abstract fun refineSupertypes(classDescriptor: ClassDescriptor): Collection<KotlinType>

    @TypeRefinement
    abstract fun refineSupertypes(classDescriptor: ClassDescriptor, moduleDescriptor: ModuleDescriptor): Collection<KotlinType>

    object Default : KotlinTypeRefiner() {
        @TypeRefinement
        override fun refineType(type: KotlinType): KotlinType = type

        @TypeRefinement
        override fun refineSupertypes(classDescriptor: ClassDescriptor): Collection<KotlinType> {
            return classDescriptor.typeConstructor.supertypes
        }

        @TypeRefinement
        override fun refineSupertypes(classDescriptor: ClassDescriptor, moduleDescriptor: ModuleDescriptor): Collection<KotlinType> {
            return classDescriptor.typeConstructor.supertypes
        }
    }
}