/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable

class FirBasedIrBuilderContext(
    val moduleDescriptor: ModuleDescriptor
) : IrGeneratorContext(IrBuiltIns(moduleDescriptor.builtIns)) {
    val sourceManager = FirSourceManager()

    val symbolTable = SymbolTable()
}