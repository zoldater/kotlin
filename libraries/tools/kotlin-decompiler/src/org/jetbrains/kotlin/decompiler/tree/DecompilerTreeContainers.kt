/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree

import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeDeclaration
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerTreeTypeParameter
import org.jetbrains.kotlin.decompiler.tree.expressions.DecompilerTreeConstructorCall
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter


interface DecompilerTreeAnnotationsContainer {
    val annotations: List<DecompilerTreeConstructorCall>
    val annotationTarget: String?
    val annotationSourcesList: List<String>
        get() = annotations
            .map { StringBuilder().also { sb -> it.produceSources(SmartPrinter(sb)) } }
            .map { sb -> annotationTarget?.let { "@$it:$sb" } ?: sb.toString() }
}

interface DecompilerTreeDeclarationContainer {
    val declarations: List<DecompilerTreeDeclaration>
}

interface DecompilerTreeStatementsContainer {
    val declarations: List<DecompilerTreeStatement>
}

interface DecompilerTreeTypeParametersContainer {
    var typeParameters: List<DecompilerTreeTypeParameter>
}