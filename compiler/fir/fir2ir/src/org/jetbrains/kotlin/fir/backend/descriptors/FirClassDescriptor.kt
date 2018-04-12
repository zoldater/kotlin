/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.Printer

class FirClassDescriptor(
    val symbol: FirClassSymbol, container: DeclarationDescriptor
) : FirAbstractSymbolBasedMemberDescriptor<FirClass>(symbol, container), ClassDescriptor {
    override fun getOriginal(): ClassDescriptor {
        return this
    }

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnsubstitutedMemberScope(): MemberScope {
        return object: MemberScope {
            override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
                TODO("not implemented")
            }

            override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
                TODO("not implemented")
            }

            override fun getFunctionNames(): Set<Name> {
                TODO("not implemented")
            }

            override fun getVariableNames(): Set<Name> {
                TODO("not implemented")
            }

            override fun getClassifierNames(): Set<Name>? {
                TODO("not implemented")
            }

            override fun printScopeStructure(p: Printer) {
                TODO("not implemented")
            }

            override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
                TODO("not implemented")
            }

            override fun getContributedDescriptors(
                kindFilter: DescriptorKindFilter,
                nameFilter: (Name) -> Boolean
            ): Collection<DeclarationDescriptor> {
                TODO("not implemented")
            }

        }
    }

    override fun getUnsubstitutedInnerClassesScope(): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStaticScope(): MemberScope {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getConstructors(): Collection<ClassConstructorDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDefaultType(): SimpleType {
        return TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope)
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getKind(): ClassKind = declaration.classKind

    override fun isCompanionObject(): Boolean = declaration.isCompanion

    override fun isData(): Boolean = declaration.isData

    override fun isInline(): Boolean = declaration.isInline


    private val receiverParameterDesc by lazy {
        LazyClassReceiverParameterDescriptor(this)
    }

    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor {
        return receiverParameterDesc
    }

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSealedSubclasses(): Collection<ClassDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTypeConstructor(): TypeConstructor {
        return object : TypeConstructor {
            override fun getParameters(): List<TypeParameterDescriptor> {
                return emptyList() // TODO:!!!
            }

            override fun getSupertypes(): Collection<KotlinType> {
                return symbol.superTypes.map { it.toKotlinType() }
            }

            override fun isFinal(): Boolean {
                TODO("not implemented")
            }

            override fun isDenotable(): Boolean {
                TODO("not implemented")
            }

            override fun getDeclarationDescriptor(): ClassifierDescriptor? {
                return this@FirClassDescriptor
            }

            override fun getBuiltIns(): KotlinBuiltIns {
                TODO("not implemented")
            }
        }
    }

    override fun isInner(): Boolean = declaration.isInner

    override fun getDeclaredTypeParameters(): List<FirTypeParameterDescriptor> {
        return emptyList() // TODO
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitClassDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }
}