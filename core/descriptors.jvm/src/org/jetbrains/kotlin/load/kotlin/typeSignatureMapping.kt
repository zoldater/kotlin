/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.kotlin.resolve.unsubstitutedUnderlyingType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlin.utils.DO_NOTHING_3

interface JvmTypeFactory<T : Any> {
    fun boxType(possiblyPrimitiveType: T): T
    fun createFromString(representation: String): T
    fun createObjectType(internalName: String): T
    fun toString(type: T): String

    val javaLangClassType: T
}

interface BuiltInChecker<T : KotlinTypeMarker> {
    fun getPrimitiveType(type: T): PrimitiveType?
    fun getPrimitiveArrayType(type: T): PrimitiveType?
    fun isUnderKotlinPackage(type: T): Boolean
    fun hasEnhancedNullability(type: T): Boolean
    fun isArray(type: T): Boolean
    fun isSuspendFunctionType(type: T): Boolean
    fun mapBuiltIn(type: T): ClassId?
    fun isKClass(type: T): Boolean

    object KotlinTypeBuiltInChecker : BuiltInChecker<KotlinType> {
        override fun getPrimitiveType(type: KotlinType): PrimitiveType? {
            return KotlinBuiltIns.getPrimitiveType(type.constructor.declarationDescriptor!!)
        }

        override fun getPrimitiveArrayType(type: KotlinType): PrimitiveType? {
            return KotlinBuiltIns.getPrimitiveArrayType(type.constructor.declarationDescriptor!!)
        }

        override fun isUnderKotlinPackage(type: KotlinType): Boolean {
            return KotlinBuiltIns.isUnderKotlinPackage(type.constructor.declarationDescriptor!!)
        }

        override fun hasEnhancedNullability(type: KotlinType): Boolean {
            return type.hasEnhancedNullability()
        }

        override fun isArray(type: KotlinType): Boolean {
            return KotlinBuiltIns.isArray(type)
        }

        override fun isSuspendFunctionType(type: KotlinType): Boolean {
            return type.isSuspendFunctionType
        }

        override fun mapBuiltIn(type: KotlinType): ClassId? {
            return JavaToKotlinClassMap.mapKotlinToJava(type.constructor.declarationDescriptor!!.fqNameUnsafe)
        }

        override fun isKClass(type: KotlinType): Boolean {
            return KotlinBuiltIns.isKClass(type.constructor.declarationDescriptor as ClassDescriptor)
        }
    }
}

private fun <T : Any> JvmTypeFactory<T>.boxTypeIfNeeded(possiblyPrimitiveType: T, needBoxedType: Boolean) =
    if (needBoxedType) boxType(possiblyPrimitiveType) else possiblyPrimitiveType

interface TypeMappingConfiguration<out T : Any> {
    fun commonSupertype(
        intersectionType: IntersectionTypeConstructor
    ): KotlinTypeMarker

    fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): T? = null
    fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String? = null
    fun processErrorType(kotlinType: KotlinTypeMarker)
    // returns null when type doesn't need to be preprocessed
    fun preprocessType(kotlinType: KotlinTypeMarker): KotlinTypeMarker? = null

    fun releaseCoroutines(): Boolean = true
}

const val NON_EXISTENT_CLASS_NAME = "error/NonExistentClass"

fun <T : Any> TypeSystemContext.mapType(
    kotlinType: KotlinTypeMarker,
    factory: JvmTypeFactory<T>,
    builtInChecker: BuiltInChecker<KotlinTypeMarker>,
    mode: TypeMappingMode,
    typeMappingConfiguration: TypeMappingConfiguration<T>,
    descriptorTypeWriter: JvmDescriptorTypeWriter<T>?,
    writeGenericType: (KotlinTypeMarker, T, TypeMappingMode) -> Unit = DO_NOTHING_3,
    isIrBackend: Boolean
): T {
    typeMappingConfiguration.preprocessType(kotlinType)?.let { newType ->
        return mapType(
            newType,
            factory,
            builtInChecker,
            mode,
            typeMappingConfiguration,
            descriptorTypeWriter,
            writeGenericType,
            isIrBackend
        )
    }

    if (builtInChecker.isSuspendFunctionType(kotlinType)) {
        return mapType(
            transformSuspendFunctionToRuntimeFunctionType(kotlinType, typeMappingConfiguration.releaseCoroutines()),
            factory, builtInChecker, mode, typeMappingConfiguration, descriptorTypeWriter,
            writeGenericType,
            isIrBackend
        )
    }

    mapBuiltInType(kotlinType, factory, builtInChecker, mode)?.let { builtInType ->
        val jvmType = factory.boxTypeIfNeeded(builtInType, mode.needPrimitiveBoxing)
        writeGenericType(kotlinType, jvmType, mode)
        return jvmType
    }

    val constructor = kotlinType.typeConstructor()

    if (constructor.isIntersection()) {
        val commonSupertype = typeMappingConfiguration.commonSupertype(constructor as IntersectionTypeConstructor) as KotlinType
        // interface In<in E>
        // open class A : In<A>
        // open class B : In<B>
        // commonSupertype(A, B) = In<A & B>
        // So replace arguments with star-projections to prevent infinite recursive mapping
        // It's not very important because such types anyway are prohibited in declarations
        return mapType(
            commonSupertype.replaceArgumentsWithStarProjections(),
            factory, builtInChecker, mode, typeMappingConfiguration, descriptorTypeWriter, writeGenericType, isIrBackend
        )
    }

//    val descriptor =
//        constructor.declarationDescriptor
//            ?: throw UnsupportedOperationException("no descriptor for type constructor of $kotlinType")

    when {
        kotlinType.isError() -> {
            val jvmType = factory.createObjectType(NON_EXISTENT_CLASS_NAME)
            typeMappingConfiguration.processErrorType(kotlinType)
            descriptorTypeWriter?.writeClass(jvmType)
            return jvmType
        }

        constructor.isClassTypeConstructor() && kotlinType is SimpleTypeMarker && builtInChecker.isArray(kotlinType) -> {
            if (kotlinType.argumentsCount() != 1) {
                throw UnsupportedOperationException("arrays must have one type argument")
            }
            val memberProjection = kotlinType.asArgumentList()[0]
            val memberType = memberProjection.getType()

            val arrayElementType: T
            if (memberProjection.getVariance() === TypeVariance.IN) {
                arrayElementType = factory.createObjectType("java/lang/Object")
                descriptorTypeWriter?.apply {
                    writeArrayType()
                    writeClass(arrayElementType)
                    writeArrayEnd()
                }
            } else {
                descriptorTypeWriter?.writeArrayType()

                arrayElementType =
                    mapType(
                        memberType, factory, builtInChecker,
                        mode.toGenericArgumentMode(memberProjection.getVariance()),
                        typeMappingConfiguration, descriptorTypeWriter, writeGenericType,
                        isIrBackend
                    )

                descriptorTypeWriter?.writeArrayEnd()
            }

            return factory.createFromString("[" + factory.toString(arrayElementType))
        }

        constructor.isClassTypeConstructor() && kotlinType is SimpleTypeMarker -> {
            // NB if inline class is recursive, it's ok to map it as wrapped
            if (kotlinType.isInlineClass() && !mode.needInlineClassWrapping) {
                val expandedType = computeExpandedTypeForInlineClass(kotlinType)
                if (expandedType != null) {
                    return mapType(
                        expandedType,
                        factory,
                        builtInChecker,
                        mode.wrapInlineClassesMode(),
                        typeMappingConfiguration,
                        descriptorTypeWriter,
                        writeGenericType,
                        isIrBackend
                    )
                }
            }

            val jvmType =
                if (mode.isForAnnotationParameter && builtInChecker.isKClass(kotlinType)) {
                    factory.javaLangClassType
                } else {
                    typeMappingConfiguration.getPredefinedTypeForClass(descriptor.original)
                        ?: run {
                            // refer to enum entries by enum type in bytecode unless ASM_TYPE is written
                            val enumClassIfEnumEntry =
                                if (descriptor.kind == ClassKind.ENUM_ENTRY)
                                    descriptor.containingDeclaration as ClassDescriptor
                                else
                                    descriptor
                            factory.createObjectType(
                                computeInternalName(
                                    enumClassIfEnumEntry.original,
                                    typeMappingConfiguration,
                                    isIrBackend
                                )
                            )
                        }
                }

            writeGenericType(kotlinType, jvmType, mode)

            return jvmType
        }

        descriptor is TypeParameterDescriptor -> {
            val type = mapType(
                descriptor.representativeUpperBound,
                factory,
                mode,
                typeMappingConfiguration,
                writeGenericType = DO_NOTHING_3,
                descriptorTypeWriter = null,
                isIrBackend = isIrBackend
            )
            descriptorTypeWriter?.writeTypeVariable(descriptor.getName(), type)
            return type
        }

        else -> throw UnsupportedOperationException("Unknown type $kotlinType")
    }
}


fun hasVoidReturnType(descriptor: CallableDescriptor): Boolean {
    if (descriptor is ConstructorDescriptor) return true
    return KotlinBuiltIns.isUnit(descriptor.returnType!!) && !TypeUtils.isNullableType(descriptor.returnType!!)
            && descriptor !is PropertyGetterDescriptor
}

private fun <T : Any> TypeSystemContext.mapBuiltInType(
    type: KotlinTypeMarker,
    typeFactory: JvmTypeFactory<T>,
    builtInChecker: BuiltInChecker<KotlinTypeMarker>,
    mode: TypeMappingMode
): T? {
    if (type !is SimpleTypeMarker || !type.isClassType()) return null

    val primitiveType = builtInChecker.getPrimitiveType(type)
    if (primitiveType != null) {
        val jvmType = typeFactory.createFromString(JvmPrimitiveType.get(primitiveType).desc)
        val isNullableInJava = type.isNullableType() || builtInChecker.hasEnhancedNullability(type)
        return typeFactory.boxTypeIfNeeded(jvmType, isNullableInJava)
    }

    val arrayElementType = builtInChecker.getPrimitiveArrayType(type)
    if (arrayElementType != null) {
        return typeFactory.createFromString("[" + JvmPrimitiveType.get(arrayElementType).desc)
    }

    if (builtInChecker.isUnderKotlinPackage(type)) {
        val classId = builtInChecker.mapBuiltIn(type)
        if (classId != null) {
            if (!mode.kotlinCollectionsToJavaCollections &&
                JavaToKotlinClassMap.mutabilityMappings.any { it.javaClass == classId }
            ) return null

            return typeFactory.createObjectType(JvmClassName.byClassId(classId).internalName)
        }
    }

    return null
}

internal fun computeUnderlyingType(inlineClassType: KotlinType): KotlinType? {
    if (!shouldUseUnderlyingType(inlineClassType)) return null

    val descriptor = inlineClassType.unsubstitutedUnderlyingType()?.constructor?.declarationDescriptor ?: return null
    return if (descriptor is TypeParameterDescriptor)
        descriptor.representativeUpperBound
    else
        inlineClassType.substitutedUnderlyingType()
}

internal fun computeExpandedTypeForInlineClass(inlineClassType: KotlinTypeMarker): KotlinTypeMarker? {
    require(inlineClassType is KotlinType)
    return computeExpandedTypeInner(inlineClassType, hashSetOf())
}

internal fun computeExpandedTypeInner(kotlinType: KotlinType, visitedClassifiers: HashSet<ClassifierDescriptor>): KotlinType? {
    val classifier = kotlinType.constructor.declarationDescriptor
        ?: throw AssertionError("Type with a declaration expected: $kotlinType")
    if (!visitedClassifiers.add(classifier)) return null

    return when {
        classifier is TypeParameterDescriptor ->
            computeExpandedTypeInner(classifier.representativeUpperBound, visitedClassifiers)
                ?.let { expandedUpperBound ->
                    if (expandedUpperBound.isNullable() || !kotlinType.isMarkedNullable)
                        expandedUpperBound
                    else
                        expandedUpperBound.makeNullable()
                }

        classifier is ClassDescriptor && classifier.isInline -> {
            // kotlinType is the boxed inline class type

            val underlyingType = kotlinType.substitutedUnderlyingType() ?: return null
            val expandedUnderlyingType = computeExpandedTypeInner(underlyingType, visitedClassifiers) ?: return null
            when {
                !kotlinType.isNullable() -> expandedUnderlyingType

                // Here inline class type is nullable. Apply nullability to the expandedUnderlyingType.

                // Nullable types become inline class boxes
                expandedUnderlyingType.isNullable() -> kotlinType

                // Primitives become inline class boxes
                KotlinBuiltIns.isPrimitiveType(expandedUnderlyingType) -> kotlinType

                // Non-null reference types become nullable reference types
                else -> expandedUnderlyingType.makeNullable()
            }
        }

        else -> kotlinType
    }
}

internal fun shouldUseUnderlyingType(inlineClassType: KotlinType): Boolean {
    val underlyingType = inlineClassType.unsubstitutedUnderlyingType() ?: return false

    return !inlineClassType.isMarkedNullable ||
            !TypeUtils.isNullableType(underlyingType) && !KotlinBuiltIns.isPrimitiveType(underlyingType)
}

fun computeInternalName(
    klass: ClassDescriptor,
    typeMappingConfiguration: TypeMappingConfiguration<*> = TypeMappingConfigurationImpl,
    isIrBackend: Boolean
): String {
    val container = if (isIrBackend) getContainer(klass.containingDeclaration) else klass.containingDeclaration

    val name = SpecialNames.safeIdentifier(klass.name).identifier
    if (container is PackageFragmentDescriptor) {
        val fqName = container.fqName
        return if (fqName.isRoot) name else fqName.asString().replace('.', '/') + '/' + name
    }

    val containerClass = container as? ClassDescriptor
        ?: throw IllegalArgumentException("Unexpected container: $container for $klass")

    val containerInternalName =
        typeMappingConfiguration.getPredefinedInternalNameForClass(containerClass) ?: computeInternalName(
            containerClass,
            typeMappingConfiguration,
            isIrBackend
        )
    return "$containerInternalName$$name"
}

private fun getContainer(container: DeclarationDescriptor?): DeclarationDescriptor? =
    container as? ClassDescriptor ?: container as? PackageFragmentDescriptor ?: container?.let { getContainer(it.containingDeclaration) }

open class JvmDescriptorTypeWriter<T : Any>(private val jvmTypeFactory: JvmTypeFactory<T>) {
    private var jvmCurrentTypeArrayLevel: Int = 0
    protected var jvmCurrentType: T? = null
        private set

    protected fun clearCurrentType() {
        jvmCurrentType = null
        jvmCurrentTypeArrayLevel = 0
    }

    open fun writeArrayType() {
        if (jvmCurrentType == null) {
            ++jvmCurrentTypeArrayLevel
        }
    }

    open fun writeArrayEnd() {
    }

    open fun writeClass(objectType: T) {
        writeJvmTypeAsIs(objectType)
    }

    protected fun writeJvmTypeAsIs(type: T) {
        if (jvmCurrentType == null) {
            jvmCurrentType =
                if (jvmCurrentTypeArrayLevel > 0) {
                    jvmTypeFactory.createFromString("[".repeat(jvmCurrentTypeArrayLevel) + jvmTypeFactory.toString(type))
                } else {
                    type
                }
        }
    }

    open fun writeTypeVariable(name: Name, type: T) {
        writeJvmTypeAsIs(type)
    }
}
