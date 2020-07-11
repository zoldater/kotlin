/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.tree.declarations.classes

import org.jetbrains.kotlin.decompiler.tree.DecompilerTreeType

class DecompilerTreeAnnotationClass(configuration: DecompilerTreeClassConfiguration) : AbstractDecompilerTreeClass(configuration) {
    override val keyword: String = "annotation class"

    override val implementedInterfaces: List<DecompilerTreeType>
        get() = super.implementedInterfaces.filterNot { it.decompile().toLowerCase() == "annotation" }
}