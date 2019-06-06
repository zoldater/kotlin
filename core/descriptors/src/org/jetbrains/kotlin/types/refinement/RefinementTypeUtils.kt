/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.refinement

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun TypeConstructor.areThereExpectSupertypesOrTypeArguments(): Boolean {
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