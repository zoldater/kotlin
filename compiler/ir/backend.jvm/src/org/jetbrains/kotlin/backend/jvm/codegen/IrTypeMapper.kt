/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.fqName
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isArrayOrNullableArray
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.DO_NOTHING_3
import org.jetbrains.org.objectweb.asm.Type

class IrTypeMapper(
    val classBuilderMode: ClassBuilderMode,
    private val moduleName: String,
    private val languageVersionSettings: LanguageVersionSettings,
    private val incompatibleClassTracker: IncompatibleClassTracker = IncompatibleClassTracker.DoNothing,
    val jvmTarget: JvmTarget = JvmTarget.DEFAULT,
    private val typePreprocessor: ((KotlinType) -> KotlinType?)? = null
) {


    @JvmOverloads
    fun mapType(
        type: IrType,
        signatureVisitor: JvmSignatureWriter? = null,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT
    ): Type = mapType(
        type, AsmTypeFactory, mode, signatureVisitor//,
//        { ktType, asmType, typeMappingMode ->
//            writeGenericType(ktType, asmType, signatureVisitor, typeMappingMode)
//        }
    )
}


fun <T : Any> mapType(
    kotlinType: IrType,
    factory: JvmTypeFactory<T>,
    mode: TypeMappingMode,
    descriptorTypeWriter: JvmDescriptorTypeWriter<T>?,
    irBuiltIns: IrBuiltIns,
    writeGenericType: (IrType, T, TypeMappingMode) -> Unit = DO_NOTHING_3
): T {

    mapBuiltInType(kotlinType, factory, mode, irBuiltIns)?.let { builtInType ->
        val jvmType = factory.boxTypeIfNeeded(builtInType, mode.needPrimitiveBoxing)
        writeGenericType(kotlinType, jvmType, mode)
        return jvmType
    }


//    val descriptor =
//        constructor.declarationDescriptor
//            ?: throw UnsupportedOperationException("no descriptor for type constructor of $kotlinType")

    when {
//        ErrorUtils.isError(descriptor) -> {
//            val jvmType = factory.createObjectType(NON_EXISTENT_CLASS_NAME)
//            typeMappingConfiguration.processErrorType(kotlinType, descriptor as ClassDescriptor)
//            descriptorTypeWriter?.writeClass(jvmType)
//            return jvmType
//        }

        descriptor is ClassDescriptor && irBuiltIns.isArray(kotlinType) -> {
            if (kotlinType.arguments.size != 1) {
                throw UnsupportedOperationException("arrays must have one type argument")
            }
            val memberProjection = kotlinType.arguments[0]
            val memberType = memberProjection.type

            val arrayElementType: T
            if (memberProjection.projectionKind === Variance.IN_VARIANCE) {
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
                        memberType, factory,
                        mode.toGenericArgumentMode(memberProjection.projectionKind),
                        typeMappingConfiguration, descriptorTypeWriter, writeGenericType,
                        isIrBackend
                    )

                descriptorTypeWriter?.writeArrayEnd()
            }

            return factory.createFromString("[" + factory.toString(arrayElementType))
        }

        descriptor is ClassDescriptor -> {
            // NB if inline class is recursive, it's ok to map it as wrapped
            if (descriptor.isInline && !mode.needInlineClassWrapping) {
                val expandedType = computeExpandedTypeForInlineClass(kotlinType)
                if (expandedType != null) {
                    return mapType(
                        expandedType,
                        factory,
                        mode.wrapInlineClassesMode(),
                        typeMappingConfiguration,
                        descriptorTypeWriter,
                        writeGenericType,
                        isIrBackend
                    )
                }
            }

            val jvmType =
                if (mode.isForAnnotationParameter && KotlinBuiltIns.isKClass(descriptor)) {
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

private fun <T : Any> JvmTypeFactory<T>.boxTypeIfNeeded(possiblyPrimitiveType: T, needBoxedType: Boolean) =
    if (needBoxedType) boxType(possiblyPrimitiveType) else possiblyPrimitiveType


private fun <T : Any> mapBuiltInType(
    type: IrType,
    typeFactory: JvmTypeFactory<T>,
    mode: TypeMappingMode,
    irBuiltIns: IrBuiltIns
): T? {
    val simpleType = type as? IrSimpleType ?: return null

    val owner = type.classifier.owner as? IrDeclarationWithName ?: return null

    (owner.descriptor as? ClassifierDescriptor)?.let {

        if (simpleType.isArrayOrNullableArray()) {
            (simpleType.arguments.first() as? IrSimpleType)?.let { arrayParameter ->
                val paramOwner = (type.classifier.owner as? IrDeclarationWithName)?.descriptor as? ClassDescriptor
                if (paramOwner != null) {
                    val ifPrimitive = getIfPrimitive(irBuiltIns, paramOwner, owner, arrayParameter)
                    if (ifPrimitive != null) {
                        return typeFactory.createFromString("[" + JvmPrimitiveType.get(ifPrimitive).desc)
                    }
                }
            }
        } else {
            getIfPrimitive(irBuiltIns, it, owner, simpleType)?.let {
                val jvmType = typeFactory.createFromString(JvmPrimitiveType.get(it).desc)
                //val isNullableInJava = TypeUtils.isNullableType(type) || type.hasEnhancedNullability()
                val isNullableInJava = simpleType.isNullable()
                return typeFactory.boxTypeIfNeeded(jvmType, isNullableInJava)
            }
        }
    }


    /*val arrayElementType = KotlinBuiltIns.getPrimitiveArrayType(simpleType)
    if (arrayElementType != null) {
        return typeFactory.createFromString("[" + JvmPrimitiveType.get(arrayElementType).desc)
    }*/


    //TODO
    if (true /*&& KotlinBuiltIns.isUnderKotlinPackage(descriptor)*/) {
        val classId = JavaToKotlinClassMap.mapKotlinToJava(owner.fqName!!.toUnsafe())
        if (classId != null) {
            if (!mode.kotlinCollectionsToJavaCollections &&
                JavaToKotlinClassMap.mutabilityMappings.any { it.javaClass == classId }
            ) return null

            return typeFactory.createObjectType(JvmClassName.byClassId(classId).internalName)
        }
    }

    return null
}

private fun <T : Any> getIfPrimitive(
    irBuiltIns: IrBuiltIns,
    it: ClassifierDescriptor,
    owner: IrDeclarationWithName,
    type: IrType
): PrimitiveType? {
    val primitiveType = irBuiltIns.getPrimitiveTypeOrNullByDescriptor(it, false)
    return if (primitiveType != null) {
        PrimitiveType.valueOf(owner.name.asString())
    } else {
        null
    }
}

