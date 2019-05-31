/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class FirPackageFragmentDescriptor(override val fqName: FirFqName, val moduleDescriptor: ModuleDescriptor) : PackageFragmentDescriptor {
    override fun getContainingDeclaration(): ModuleDescriptor {
        return moduleDescriptor
    }


    override fun getMemberScope(): MemberScope {
        return MemberScope.Empty
    }

    override fun getOriginal(): DeclarationDescriptorWithSource {
        return this
    }

    override fun getName(): FirName {
        return fqName.shortName()
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
        TODO("not implemented")
    }

    override fun getSource(): SourceElement {
        TODO("not implemented")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        TODO("not implemented")
    }

    override val annotations: Annotations
        get() = Annotations.EMPTY

}