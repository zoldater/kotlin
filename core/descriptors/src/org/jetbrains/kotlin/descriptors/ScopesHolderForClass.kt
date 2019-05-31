/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.types.refinement.getOrPutScopeForClass
import org.jetbrains.kotlin.types.refinement.isRefinementNeededForTypeConstructor

class ScopesHolderForClass<T : MemberScope> private constructor(
    private val classDescriptor: ClassDescriptor,
    storageManager: StorageManager,
    private val scopeFactory: (ModuleDescriptor) -> T
) {
    private val scopeForOwnerModule by storageManager.createLazyValue { scopeFactory(classDescriptor.module) }

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @UseExperimental(TypeRefinement::class)
    fun getScope(moduleDescriptor: ModuleDescriptor): T {
        if (classDescriptor.module === moduleDescriptor) return scopeForOwnerModule

        if (!moduleDescriptor.isRefinementNeededForTypeConstructor(classDescriptor.typeConstructor)) return scopeForOwnerModule
        return moduleDescriptor.getOrPutScopeForClass(classDescriptor) { scopeFactory(moduleDescriptor) }
    }

    companion object {
        fun <T : MemberScope> create(
            classDescriptor: ClassDescriptor,
            storageManager: StorageManager,
            scopeFactory: (ModuleDescriptor) -> T
        ): ScopesHolderForClass<T> {
            return ScopesHolderForClass(classDescriptor, storageManager, scopeFactory)
        }
    }
}
