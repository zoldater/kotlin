/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeDeclaration
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeTypeParameter
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeAnnotationConstructorCall
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty


interface DecompilerTreeAnnotationsContainer {
    val annotations: List<DecompilerTreeAnnotationConstructorCall>
    val annotationTarget: String?
    val annotationSourcesList: List<String>
        get() = annotations
            .map { it.decompile() }
            .map { sb -> annotationTarget?.let { "@$it:$sb" } ?: sb }
}

interface DecompilerTreeDeclarationContainer {
    val declarations: List<DecompilerTreeDeclaration>
}

interface DecompilerTreeStatementsContainer {
    val statements: List<DecompilerTreeStatement>
}

interface DecompilerTreeTypeParametersContainer {
    val typeParameters: List<DecompilerTreeTypeParameter>

    val typeParametersForPrint: String?
        get() = typeParameters.ifNotEmpty { joinToString(", ", "<", ">") { it.decompile() } }
}

interface DecompilerTreeTypeArgumentsContainer {
    val typeArguments: List<DecompilerTreeType>

    val typeArgumentsForPrint: String?
        get() = typeArguments.ifNotEmpty { joinToString(", ", "<", ">") { it.decompile() } }
}