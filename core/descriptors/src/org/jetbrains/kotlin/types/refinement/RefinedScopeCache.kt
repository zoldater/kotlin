/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.refinement

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.TypeConstructor

sealed class RefinedScopeCache(protected val moduleDescriptor: ModuleDescriptor) {
    @TypeRefinementInternal
    abstract fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean

    @TypeRefinementInternal
    abstract fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S
}

internal class RefinedScopeCacheImpl(moduleDescriptor: ModuleDescriptor, storageManager: StorageManager) : RefinedScopeCache(moduleDescriptor) {
    private val _isRefinementNeededForTypeConstructor =
        storageManager.createMemoizedFunction(TypeConstructor::areThereExpectSupertypesOrTypeArguments)
    private val scopes = storageManager.createCacheWithNotNullValues<ClassDescriptor, MemberScope>()

    @TypeRefinementInternal
    override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
        return _isRefinementNeededForTypeConstructor.invoke(typeConstructor)
    }

    @TypeRefinementInternal
    override fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S {
        @Suppress("UNCHECKED_CAST")
        return scopes.computeIfAbsent(classDescriptor, compute) as S
    }
}

/**
 * We can think that if there is no RefinedScopeCache cache in module descriptor and EmptyRefinedScopeCache
 *   is using, then type refinement is disabled
 */
private class EmptyRefinedScopeCache(moduleDescriptor: ModuleDescriptor) : RefinedScopeCache(moduleDescriptor) {
    @TypeRefinementInternal
    override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean = false

    @TypeRefinementInternal
    override fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S = compute()
}

@TypeRefinementInternal
internal val refinementCacheCapability = ModuleDescriptor.Capability<RefinedScopeCache>("RefinedScopeCache")

@TypeRefinementInternal
private val ModuleDescriptor.refinedScopeCache: RefinedScopeCache
    get() = getCapability(refinementCacheCapability) ?: EmptyRefinedScopeCache(this)

@TypeRefinement
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@UseExperimental(TypeRefinementInternal::class)
fun <S : MemberScope> ModuleDescriptor.getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S =
    refinedScopeCache.getOrPutScopeForClass(classDescriptor, compute)

@TypeRefinement
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@UseExperimental(TypeRefinementInternal::class)
fun ModuleDescriptor.isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
    return refinedScopeCache.isRefinementNeededForTypeConstructor(typeConstructor)
}