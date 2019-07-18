/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object ForwardDeclarationsFqNames {

    val packageName = FqName("kotlinx.cinterop")

    private val cNames = FqName("cnames")
    val cNamesStructs = cNames.child(Name.identifier("structs"))

    private val objCNames = FqName("objcnames")
    val objCNamesClasses = objCNames.child(Name.identifier("classes"))
    val objCNamesProtocols = objCNames.child(Name.identifier("protocols"))
}
