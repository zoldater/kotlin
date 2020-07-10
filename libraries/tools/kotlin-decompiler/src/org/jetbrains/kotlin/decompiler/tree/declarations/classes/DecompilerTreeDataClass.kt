/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

class DecompilerTreeDataClass(configurator: DecompilerTreeClassConfigurator) : AbstractDecompilerTreeClass(configurator) {

    override val keyword: String = "data class"

    override val primaryConstructor: DecompilerTreePrimaryConstructor?
        get() = declarations.filterIsInstance<DecompilerTreePrimaryConstructor>().firstOrNull()

    private val primaryConstructorPropertiesOrNull
        get() = primaryConstructor?.valueParameters?.mapNotNull { it as? DecompilerTreePropertyValueParameter }
            ?.mapNotNull { it.relatedProperty }

    override val methods: List<DecompilerTreeSimpleFunction>
        get() = super.methods.filterNot { it.element.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER }

    override val properties: List<DecompilerTreeProperty>
        get() = primaryConstructorPropertiesOrNull?.let { super.properties - it } ?: super.properties

    override val printableDeclarations: List<DecompilerTreeDeclaration>
        get() = (properties + methods + otherPrintableDeclarations)

}