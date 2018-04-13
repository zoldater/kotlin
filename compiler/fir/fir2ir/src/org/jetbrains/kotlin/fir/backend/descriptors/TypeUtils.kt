/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.*

class StubTypeConstructor(val classifier: ClassifierDescriptor? = null) : TypeConstructor {
    override fun getParameters(): List<TypeParameterDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSupertypes(): Collection<KotlinType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isFinal(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isDenotable(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDeclarationDescriptor(): ClassifierDescriptor? {
        return classifier
    }

    override fun getBuiltIns(): KotlinBuiltIns {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

fun ConeKotlinType.toKotlinType(classifier: ClassifierDescriptor? = null, nullable: Boolean = false): SimpleType {
    return KotlinTypeFactory.simpleType(
        annotations = Annotations.EMPTY, // TODO
        constructor = StubTypeConstructor(classifier),
        arguments = typeArguments.map {
            when (it) {
                StarProjection ->
                    StarProjectionImpl(typeParameter = TODO())
                is ConeKotlinTypeProjectionIn ->
                    TypeProjectionImpl(Variance.IN_VARIANCE, it.type.toKotlinType())
                is ConeKotlinTypeProjectionOut ->
                    TypeProjectionImpl(Variance.OUT_VARIANCE, it.type.toKotlinType())
                is ConeKotlinType ->
                    TypeProjectionImpl(Variance.INVARIANT, it.toKotlinType())
            }
        },
        nullable = nullable
    )
}

fun FirType.toKotlinType(classifier: ClassifierDescriptor? = null): SimpleType {
    val nullable = (this as? FirTypeWithNullability)?.isNullable == true
    if (this is FirResolvedType) {
        val coneType = this.type
        return coneType.toKotlinType(classifier, nullable)
    }
    return UnresolvedType(
        presentableName = this.render(),
        constructor = StubTypeConstructor(),
        memberScope = ErrorUtils.createErrorScope("Unresolved FIR type", true),
        arguments = emptyList(),
        isMarkedNullable = nullable
    )
}