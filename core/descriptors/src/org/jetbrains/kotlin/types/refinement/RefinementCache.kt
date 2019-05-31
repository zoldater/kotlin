/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.refinement

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class RefinementCache(protected val moduleDescriptor: ModuleDescriptor) {
    abstract fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean
    abstract fun refineOrGetType(type: SimpleType, refinedTypeFactory: (ModuleDescriptor) -> SimpleType?): SimpleType
}

internal class RefinementCacheImpl(moduleDescriptor: ModuleDescriptor, storageManager: StorageManager) : RefinementCache(moduleDescriptor) {
    private val _isRefinementNeededForTypeConstructor =
        storageManager.createMemoizedFunction(TypeConstructor::areThereExpectSupertypesOrTypeArguments)
    private val refinedTypeCache = HashMap<SimpleType, SimpleType>()

    override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
        return _isRefinementNeededForTypeConstructor.invoke(typeConstructor)
    }

    override fun refineOrGetType(type: SimpleType, refinedTypeFactory: (ModuleDescriptor) -> SimpleType?): SimpleType {
        refinedTypeCache[type]?.let { return it }
        val refinedType = refinedTypeFactory(moduleDescriptor) ?: type
        refinedTypeCache[type] = refinedType
        return refinedType
    }
}

private class EmptyRefinementCache(moduleDescriptor: ModuleDescriptor) : RefinementCache(moduleDescriptor) {
    override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
        return typeConstructor.areThereExpectSupertypesOrTypeArguments()
    }

    override fun refineOrGetType(type: SimpleType, refinedTypeFactory: (ModuleDescriptor) -> SimpleType?): SimpleType {
        return refinedTypeFactory(moduleDescriptor) ?: type
    }
}

internal val refinementCacheCapability = ModuleDescriptor.Capability<RefinementCache>("RefinementCache")

val ModuleDescriptor.refinementCache: RefinementCache
    get() = getCapability(refinementCacheCapability) ?: EmptyRefinementCache(this)

private fun TypeConstructor.areThereExpectSupertypesOrTypeArguments(): Boolean {
    var result = false
    DFS.dfs(
        listOf(this),
        DFS.Neighbors(TypeConstructor::allDependentTypeConstructors),
        DFS.VisitedWithSet(),
        object : DFS.AbstractNodeHandler<TypeConstructor, Unit>() {
            override fun beforeChildren(current: TypeConstructor): Boolean {
                if (current.isExpectClass()) {
                    result = true
                    return false
                }
                return true
            }

            override fun result() = Unit
        }
    )

    return result
}

private val TypeConstructor.allDependentTypeConstructors: Collection<TypeConstructor>
    get() = when (this) {
        is NewCapturedTypeConstructor -> {
            supertypes.map { it.constructor } + projection.type.constructor
        }
        else -> supertypes.map { it.constructor } + parameters.map { it.typeConstructor }
    }

private fun TypeConstructor.isExpectClass() =
    declarationDescriptor?.safeAs<ClassDescriptor>()?.isExpect == true