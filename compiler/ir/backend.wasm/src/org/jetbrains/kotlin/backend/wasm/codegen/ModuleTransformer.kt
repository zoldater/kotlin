/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.hasExcludedFromCodegenAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasSkipRTTIAnnotation
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsAssignment
import org.jetbrains.kotlin.ir.backend.js.utils.functionSignature
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.JsArrayLiteral
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull

val IrClass.superClasses: List<IrClass>
    get() = superTypes.map { it.classifierOrFail.owner as IrClass }

class IrModuleToWasm(private val backendContext: WasmBackendContext) {

    private val anyClass = backendContext.irBuiltIns.anyClass.owner

    fun generateModule(module: IrModuleFragment): WasmCompilerResult {
        val packageFragments = module.files + listOf(backendContext.internalPackageFragment)
        val irDeclarations = packageFragments.flatMap { it.declarations }
        val nameTable = generateWatTopLevelNames(packageFragments)
        val typeInfo = collectTypeInfo(irDeclarations, backendContext)

        val wasmTypeNames = generateWatTypeNames(packageFragments)
        val context = WasmCodegenContext(nameTable, wasmTypeNames, backendContext, typeInfo)


        val virtFuns = mutableListOf<String>()
        for ((f, id) in typeInfo.virtualFunctionIds) {
            virtFuns.add(id, context.getGlobalName(f))
        }

        val funcRefTable = WasmFuncrefTable(virtFuns)

        val wasmDeclarations = irDeclarations.mapNotNull { it.accept(DeclarationTransformer(), context) }
        val exports = generateExports(module, context)
        val namedTypes = generateNamedTypes(module, context)
        val wasmStart = WasmStart(context.getGlobalName(backendContext.startFunction))
        val wasmModule = WasmModule(context.imports + namedTypes + funcRefTable + wasmDeclarations + typeInfo.datas + listOf(wasmStart) + exports)
        val wat = wasmModuleToWat(wasmModule)
        return WasmCompilerResult(wat, generateStringLiteralsSupport(context.stringLiterals))
    }

    private fun collectTypeInfo(
        irDeclarations: List<IrDeclaration>,
        backendContext: WasmBackendContext
    ): WasmTypeInfo {
        val typeInfo = WasmTypeInfo(backendContext)
        val classes = irDeclarations
            .filterIsInstance<IrClass>()
            .filter { !it.isAnnotationClass && !it.hasSkipRTTIAnnotation() && !it.hasExcludedFromCodegenAnnotation() }

        val classesSorted = DFS.topologicalOrder(classes) { it.superClasses }.reversed()

        var classId = 1
        var ifaceId = 1

        val lmClassMetadata = mutableListOf<LMElement>()

        for ((i, k) in classesSorted.withIndex()) {
            if (k.isInterface) {
                typeInfo.interfaces[k] = InterfaceMetadata(ifaceId++)
                continue
            }

            val superClasses = k.superClasses
            val superClassInfo: ClassMetadata
            val lmSuperType = if (k.defaultType.isAny()) {
                superClassInfo = ClassMetadata(0, null, emptyList(), emptyList(), emptyList())
                LMFieldI32("Any's empty super class", 0)
            } else {
                val superClass: IrClass = superClasses.singleOrNull { !it.isInterface } ?: anyClass
                superClassInfo = typeInfo.classes[superClass]!!
                LMFieldI32("Super class", typeInfo.classes[superClass]!!.id)
            }

            fun IrClass.allSuperClasses(): List<IrClass> =
                (this.superClasses + this.superClasses.flatMap { it.allSuperClasses() }).distinct()

            val implementedInterfaces = k.allSuperClasses().filter { it.isInterface }

            val lmImplementedInterfacesData = LMArrayI32(
                "data",
                implementedInterfaces.map { typeInfo.interfaces[it]!!.id }
            )
            val lmImplementedInterfacesSize = LMFieldI32(
                "size",
                lmImplementedInterfacesData.value.size
            )

            val lmImplementedInterfaces = LMStruct(
                "Implemented interfaces array", listOf(lmImplementedInterfacesSize, lmImplementedInterfacesData)
            )

            val virtualFunctions = k.declarations
                .filterIsInstance<IrSimpleFunction>()
                .map { it.realOverrideTarget }
                .filter { it.isOverridableOrOverrides }

            for (vf in virtualFunctions) {
                val nextId = typeInfo.virtualFunctionIds.size
                typeInfo.virtualFunctionIds.getOrPut(vf) {
                    nextId
                }
            }

            val signatureToVirtualFunction = virtualFunctions.associateBy { functionSignature(it) }

            val inheritedVirtualMethods: List<VirtualMethodMetadata> = superClassInfo.virtualMethods.map { vm ->
                VirtualMethodMetadata(signatureToVirtualFunction[vm.signature]!!, vm.signature)
            }

            val newVirtualMethods: List<VirtualMethodMetadata> = signatureToVirtualFunction
                .filterKeys { it !in superClassInfo.virtualMethodsSignatures }
                .map {
                    VirtualMethodMetadata(it.value, it.key)
                }

            val allVirtualMethods = inheritedVirtualMethods + newVirtualMethods

            val lmVirtualFunctionsSize = LMFieldI32(
                "V-table length",
                allVirtualMethods.size
            )

            val lmVtable = LMArrayI32(
                "V-table",
                allVirtualMethods.map { typeInfo.virtualFunctionIds[it.function]!! }
            )

            val lmSignatures = LMArrayI32(
                "Signatures Stub",
                List<Int>(allVirtualMethods.size) { -1 }
            )


            val classMetadata = ClassMetadata(
                classId,
                superClass = superClassInfo,
                fields = superClassInfo.fields + k.declarations.filterIsInstance<IrField>().map { it.symbol },
                interfaces = implementedInterfaces.map { typeInfo.interfaces[it]!! },
                virtualMethods = allVirtualMethods
            )

            typeInfo.classes[k] = classMetadata

            val classLmElements = listOf(lmSuperType, lmVirtualFunctionsSize, lmVtable, lmSignatures, lmImplementedInterfaces)
            val classLmStruct = LMStruct("Class TypeInfo: $classId ${k.fqNameWhenAvailable} ", classLmElements)
            typeInfo.datas.add(WasmData(classId, classLmStruct.toBytes()))

            lmClassMetadata.add(classLmStruct)
            println("class $i:  ${k.fqNameWhenAvailable}")
            classId += classLmStruct.sizeInBytes
        }

        val virtFuns = mutableListOf<IrSimpleFunction>()
        for ((f, id) in typeInfo.virtualFunctionIds) {
            virtFuns.add(id, f)
        }

        println("\n Virtual functions")
        for ((id, f) in virtFuns.withIndex()) {
            println("$id  -> ${f.fqNameWhenAvailable}")
        }
        println("----")

        val lm = LMStruct("TypeInfo", lmClassMetadata)
        println(lm.dump())
        println("Bytes: ${lm.toBytes().contentToString()}")
        println("Bytes Data: ${lm.toBytes().toWatData()}")
        return typeInfo
    }

    private fun generateStringLiteralsSupport(literals: List<String>): String {
        return JsBlock(
            jsAssignment(
                JsNameRef("stringLiterals", "runtime"),
                JsArrayLiteral(literals.map { JsStringLiteral(it) })
            ).makeStmt()
        ).toString()
    }

    private fun generateNamedTypes(module: IrModuleFragment, context: WasmCodegenContext): List<WasmNamedType> {
        val namedTypes = mutableListOf<WasmNamedType>()
        module.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                if (element is IrAnnotationContainer && element.hasExcludedFromCodegenAnnotation()) return
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                super.visitSimpleFunction(declaration)
                namedTypes.add(context.wasmFunctionType(declaration))
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration.hasSkipRTTIAnnotation()) return
                super.visitClass(declaration)
                if (declaration.kind == ClassKind.CLASS || declaration.kind == ClassKind.OBJECT) {
                    namedTypes.add(context.wasmStructType(declaration))
                }
            }
        })

        return namedTypes
    }


    private fun generateExports(module: IrModuleFragment, context: WasmCodegenContext): List<WasmExport> {
        val exports = mutableListOf<WasmExport>()
        for (file in module.files) {
            for (declaration in file.declarations) {
                exports.addIfNotNull(generateExport(declaration, context))
            }
        }
        return exports
    }

    private fun generateExport(declaration: IrDeclaration, context: WasmCodegenContext): WasmExport? {
        if (declaration !is IrDeclarationWithVisibility ||
            declaration !is IrDeclarationWithName ||
            declaration !is IrSimpleFunction ||
            declaration.visibility != Visibilities.PUBLIC
        ) {
            return null
        }

        if (!declaration.isExported(context))
            return null

        val internalName = context.getGlobalName(declaration)
        val exportedName = sanitizeName(declaration.name.identifier)

        return WasmExport(
            wasmName = internalName,
            exportedName = exportedName,
            kind = WasmExport.Kind.FUNCTION
        )
    }

}

fun IrFunction.isExported(context: WasmCodegenContext): Boolean =
    fqNameWhenAvailable in context.backendContext.additionalExportedDeclarations
