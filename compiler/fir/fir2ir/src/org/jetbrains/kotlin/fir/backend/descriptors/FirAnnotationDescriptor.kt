/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.fir.backend.psi.toSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType

class FirAnnotationDescriptor(val call: FirAnnotationCall) : AnnotationDescriptor {
    override val type: KotlinType = call.annotationType.toKotlinType()

    override val allValueArguments: Map<Name, ConstantValue<*>>
        get() = TODO()

    override val source: SourceElement = call.toSourceElement()
}