/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fir.backend.builders.IrModuleBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

class IrBuilder {

    fun generateModule(moduleDescriptor: ModuleDescriptor, files: Collection<FirFile>): IrModuleFragment {
        val context = createBuilderContext(moduleDescriptor)
        return generateModuleFragment(context, files)
    }

    fun createBuilderContext(moduleDescriptor: ModuleDescriptor) = FirBasedIrBuilderContext(moduleDescriptor)

    fun generateModuleFragment(context: FirBasedIrBuilderContext, files: Collection<FirFile>): IrModuleFragment {
        val moduleBuilder = IrModuleBuilder(context)
        val irModule = moduleBuilder.generateModuleFragmentWithoutDependencies(files)
        irModule.patchDeclarationParents()
        return irModule
    }
}