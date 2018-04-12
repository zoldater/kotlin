/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor

abstract class FirAbstractPropertyAccessorDescriptor(
    val accessor: FirPropertyAccessor,
    val container: FirPropertyDescriptor
) : PropertyAccessorDescriptor {

    override fun getCorrespondingProperty(): FirPropertyDescriptor = container

    override fun getModality(): Modality {
        return container.modality
    }

    override fun getVisibility(): Visibility {
        return accessor.visibility
    }
}