/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager

abstract class AbstractTypeConstructor(storageManager: StorageManager) : TypeConstructor {
    override fun getSupertypes(): List<KotlinType> = supertypesWithoutCyclesLazyValue()

    private var allSupertypesLazyValue: NotNullLazyValue<Collection<KotlinType>> =
        storageManager.createRecursionTolerantLazyValue(
            computable = { computeSupertypes() },
            onRecursiveCall = { listOf(ErrorUtils.ERROR_TYPE_FOR_LOOP_IN_SUPERTYPES) }
        )

    private var supertypesWithoutCyclesLazyValue: NotNullLazyValue<List<KotlinType>> =
        storageManager.createRecursionTolerantLazyValue(
            computable = { computeDisconnectedSupertypes(allSupertypesLazyValue()) },
            onRecursiveCall = { listOf(ErrorUtils.ERROR_TYPE_FOR_LOOP_IN_SUPERTYPES) }
        )

    private fun computeDisconnectedSupertypes(allSupertypes: Collection<KotlinType>): List<KotlinType> {
        // It's important that loops disconnection begins in post-compute phase, because it guarantees that
        // when we start calculation supertypes of supertypes (for computing neighbours), they start their disconnection loop process
        // either, and as we want to report diagnostic about loops on all declarations they should see consistent version of 'allSupertypes'
        val resultWithoutCycles =
            supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
                this, allSupertypes,
                { it.computeNeighbours(useCompanions = false) },
                { reportSupertypeLoopError(it) }
            ).ifEmpty {
                listOfNotNull(defaultSupertypeIfEmpty())
            }

        // We also check if there are a loop with additional edges going from owner of companion to
        // the companion itself.
        // Note that we use already disconnected types to not report two diagnostics on cyclic supertypes
        supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
            this, resultWithoutCycles,
            { it.computeNeighbours(useCompanions = true) },
            { reportScopesLoopError(it) }
        )

        return (resultWithoutCycles as? List<KotlinType>) ?: resultWithoutCycles.toList()
    }

    private fun TypeConstructor.computeNeighbours(useCompanions: Boolean): Collection<KotlinType> =
        (this as? AbstractTypeConstructor)?.let { abstractClassifierDescriptor ->
            abstractClassifierDescriptor.allSupertypesLazyValue() +
                    abstractClassifierDescriptor.getAdditionalNeighboursInSupertypeGraph(useCompanions)
        } ?: supertypes

    protected abstract fun computeSupertypes(): Collection<KotlinType>
    protected abstract val supertypeLoopChecker: SupertypeLoopChecker
    protected open fun reportSupertypeLoopError(type: KotlinType) {}

    // TODO: overload in AbstractTypeParameterDescriptor?
    protected open fun reportScopesLoopError(type: KotlinType) {}

    protected open fun getAdditionalNeighboursInSupertypeGraph(useCompanions: Boolean): Collection<KotlinType> = emptyList()
    protected open fun defaultSupertypeIfEmpty(): KotlinType? = null

    // Only for debugging
    fun renderAdditionalDebugInformation(): String = "supertypes=${allSupertypesLazyValue.renderDebugInformation()}"
}
