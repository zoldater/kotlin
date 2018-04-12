/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

class FirSourceElement(private val fir: FirElement) : PsiSourceElement {
    override val psi: PsiElement?
        get() = fir.psi
}

fun FirElement?.toSourceElement() = this?.let { FirSourceElement(it) } ?: SourceElement.NO_SOURCE

val FirElement.startOffset get() = this.psi!!.startOffset

val FirElement.endOffset get() = this.psi!!.endOffset