/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.library.isInterop
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion

sealed class KonanModuleOrigin {
    companion object {
        val CAPABILITY = ModuleDescriptor.Capability<KonanModuleOrigin>("KonanModuleOrigin")
    }
}

sealed class CompiledKonanModuleOrigin : KonanModuleOrigin()

class DeserializedKonanModuleOrigin(val library: BaseKotlinLibrary) : CompiledKonanModuleOrigin()

object CurrentKonanModuleOrigin : CompiledKonanModuleOrigin()

object SyntheticModulesOrigin : KonanModuleOrigin()

private fun KonanModuleOrigin.isInteropLibrary(): Boolean = when (this) {
    is DeserializedKonanModuleOrigin -> library.isInterop
    CurrentKonanModuleOrigin, SyntheticModulesOrigin -> false
}

val KonanModuleOrigin.moduleCapabilities
    get() = mapOf(
        KonanModuleOrigin.CAPABILITY to this,
        ImplicitIntegerCoercion.MODULE_CAPABILITY to isInteropLibrary()
    )
