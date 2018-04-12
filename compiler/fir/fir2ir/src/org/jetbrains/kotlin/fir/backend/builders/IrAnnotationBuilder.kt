/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.builders

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.fir.backend.FirBasedIrBuilderContext
import org.jetbrains.kotlin.ir.expressions.IrCall

class IrAnnotationBuilder(val context: FirBasedIrBuilderContext) {
    fun generateAnnotationConstructorCall(descriptor: AnnotationDescriptor): IrCall {
        TODO()
    }
}
