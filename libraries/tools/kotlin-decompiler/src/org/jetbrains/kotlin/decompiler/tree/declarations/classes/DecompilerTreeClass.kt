/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeDeclaration
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeTypeParameter
import org.jetbrains.kotlin.decompiler.tree.declarations.AbstractDecompilerTreeValueParameter
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrClass

class DecompilerTreeClass(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: AbstractDecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {
    override val keyword: String = "class"

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, initSections, secondaryConstructors, methods, otherPrintableDeclarations).flatten()

}