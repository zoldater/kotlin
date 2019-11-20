/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.types.IrType

class MagicBoxImpl : IMagicBox {
    private val localDeclarationsWithNameSet = mutableSetOf<IrDeclarationWithName>()
    private val scopeWithDeclarationReferencesMap = mutableMapOf<String, Set<IrDeclarationReference>>()

    override fun putDeclarationWithName(irDeclarationWithName: IrDeclarationWithName) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putCalledDeclarationReference(scopeList: List<String>, irDeclarationReference: IrDeclarationReference) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun obtainDeclarationReferenceDescription(scopeList: List<String>, irDeclarationReference: IrDeclarationReference) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun obtainImportStatementsList(): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun obtainTypeDescriptionForScope(scopeList: List<String>, irType: IrType): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}