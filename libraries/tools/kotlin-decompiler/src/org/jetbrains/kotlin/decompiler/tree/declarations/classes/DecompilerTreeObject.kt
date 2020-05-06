/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeDeclaration
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeTypeParameter
import org.jetbrains.kotlin.decompiler.tree.declarations.AbstractDecompilerTreeValueParameter
import org.jetbrains.kotlin.decompiler.tree.declarations.name
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class DecompilerTreeObject(
    element: IrClass,
    declarations: List<DecompilerTreeDeclaration>,
    annotations: List<DecompilerTreeAnnotationConstructorCall>,
    override var typeParameters: List<DecompilerTreeTypeParameter>,
    override val thisReceiver: AbstractDecompilerTreeValueParameter?,
    superTypes: List<DecompilerTreeType>
) : AbstractDecompilerTreeClass(element, declarations, annotations, superTypes) {

    override val keyword: String = "object"

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = listOf(properties, initSections, methods, otherPrintableDeclarations).flatten()

    override val nameIfExists: String? = element.name().takeIf { it != "<no name provided>" || it != "Companion" }

    override val nameWithPrimaryCtorDecompiled: String?
        get() = listOfNotNull(computeModifiersAndName, typeParametersForPrint).ifNotEmpty { joinToString("") }
}