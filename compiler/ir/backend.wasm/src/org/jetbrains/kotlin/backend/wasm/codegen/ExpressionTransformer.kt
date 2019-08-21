/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.getWasmInstructionAnnotation
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isSubtypeOf
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*

class ExpressionTransformer : BaseTransformer<WasmInstruction, WasmCodegenContext> {
    override fun visitVararg(expression: IrVararg, data: WasmCodegenContext): WasmInstruction {
        TODO("Support arrays")
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: WasmCodegenContext): WasmInstruction =
        body.expression.accept(this, data)

    override fun visitFunctionReference(expression: IrFunctionReference, data: WasmCodegenContext): WasmInstruction {
        TODO("?")
    }

    override fun <T> visitConst(expression: IrConst<T>, data: WasmCodegenContext): WasmInstruction {
        return when (val kind = expression.kind) {
            is IrConstKind.Null -> WasmRefNull
            is IrConstKind.String -> {
                val value = kind.valueOf(expression)
                val index = data.stringLiterals.size
                data.stringLiterals.add(value)
                val funName = data.getGlobalName(data.backendContext.wasmSymbols.stringGetLiteral.owner)
                val operand = WasmI32Const(index)
                WasmCall(funName, listOf(operand))
            }
            is IrConstKind.Boolean -> WasmI32Const(if (kind.valueOf(expression)) 1 else 0)
            is IrConstKind.Byte -> WasmI32Const(kind.valueOf(expression).toInt())
            is IrConstKind.Short -> WasmI32Const(kind.valueOf(expression).toInt())
            is IrConstKind.Int -> WasmI32Const(kind.valueOf(expression))
            is IrConstKind.Long -> WasmI64Const(kind.valueOf(expression))
            is IrConstKind.Char -> WasmI32Const(kind.valueOf(expression).toInt())
            is IrConstKind.Float -> WasmF32Const(kind.valueOf(expression))
            is IrConstKind.Double -> WasmF64Const(kind.valueOf(expression))
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: WasmCodegenContext): WasmInstruction {
        TODO("Implement kotlin.String")
    }

    override fun visitGetField(expression: IrGetField, data: WasmCodegenContext): WasmInstruction {
        val field = expression.symbol.owner
        val receiver = expression.receiver
        if (receiver != null) {
            val fieldClass = field.parentAsClass
            val fieldId = getFieldId(field, data)
            return WasmStructGet(
                data.getStructTypeName(fieldClass), fieldId,
                expressionToWasmInstruction(receiver, data)
            )
        }

        val fieldName = data.getGlobalName(field)
        return WasmGetGlobal(fieldName)
    }

    override fun visitGetValue(expression: IrGetValue, data: WasmCodegenContext): WasmInstruction =
        WasmGetLocal(data.getLocalName(expression.symbol.owner))

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: WasmCodegenContext): WasmInstruction {
        TODO("IrGetObjectValue ${expression.dump()}")
    }

    private fun getFieldId(field: IrField, context: WasmCodegenContext): Int {
        val fieldClass = field.parentAsClass
        val classInfo = context.typeInfo.classes[fieldClass]!!
        return classInfo.fields.indexOf(field.symbol)
    }

    override fun visitSetField(expression: IrSetField, data: WasmCodegenContext): WasmInstruction {
        val field = expression.symbol.owner
        val fieldName = data.getGlobalName(expression.symbol.owner)
        val receiver = expression.receiver
        val wasmWalue = expressionToWasmInstruction(expression.value, data)
        if (receiver != null) {
            val fieldClass = field.parentAsClass
            val classInfo = data.typeInfo.classes[fieldClass]
            val fieldId = getFieldId(field, data)
            return WasmStructSet(
                data.getStructTypeName(fieldClass), fieldId,
                expressionToWasmInstruction(receiver, data), wasmWalue
            )
        }

        return WasmSetGlobal(fieldName, wasmWalue)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: WasmCodegenContext): WasmInstruction {
        val fieldName = data.getLocalName(expression.symbol.owner)
        val value = expression.value.accept(this, data)
        return WasmSetLocal(fieldName, value)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: WasmCodegenContext): WasmInstruction {
        val klass = expression.symbol.owner.parentAsClass
        val structTypeName = data.getStructTypeName(klass)
        val klassInfo = data.typeInfo.classes[klass]!!
        val klassId = klassInfo.id

        val initialValues = klassInfo.fields.map { defaultInitializerForType(data.transformType(it.owner.type)) }.toMutableList()
        initialValues[0] = WasmI32Const(klassId)
        // TODO: Init id

        return WasmBlock(
            resultType = data.transformType(expression.type),
            instructions = listOf(
                WasmStructNew(structTypeName, initialValues),
                transformCall(expression, data)
            )
        )
    }

    override fun visitCall(expression: IrCall, data: WasmCodegenContext): WasmInstruction {
        return transformCall(expression, data)
    }

    private fun transformCall(expression: IrFunctionAccessExpression, data: WasmCodegenContext, additionalArgument: WasmInstruction? = null): WasmInstruction {
        val function = expression.symbol.owner.realOverrideTarget
        val valueArgs = (0 until expression.valueArgumentsCount).mapNotNull { expression.getValueArgument(it) }
        val irArguments = listOfNotNull(expression.dispatchReceiver, expression.extensionReceiver) + valueArgs
        val wasmArguments = listOfNotNull(additionalArgument) + irArguments.map { expressionToWasmInstruction(it, data) }

        if (function is IrSimpleFunction && function.isOverridable) {
            val klass = function.parentAsClass
            if (!klass.isInterface) {
                val classMetadata = data.typeInfo.classes[klass]!!
                val tableIndex = classMetadata.virtualMethods.map { it.function }.indexOf(function)
                val functionType = data.getFunctionTypeName(function)

                return WasmCallIdirect(functionType, wasmArguments + listOf(WasmI32Const(tableIndex)))
            } else {
                return WasmUnreachable
                TODO("Support interface calls ${expression.dump()}")
            }
        }

        val wasmInstruction = function.getWasmInstructionAnnotation()
        if (wasmInstruction != null) {
            if (wasmInstruction == "nop") {
                return wasmArguments.single()
            }
            return WasmSimpleInstruction(wasmInstruction, wasmArguments)
        }

        if (function.symbol == data.backendContext.wasmSymbols.wasmClassId) {
            val klass = expression.getTypeArgument(0)!!.getClass() ?: error("No class given for wasmClassId intrinsic")
            assert(!klass.isInterface)
            val klassMetadata = data.typeInfo.classes[klass]
            if (klassMetadata == null) {
                println("No metadata for class ${klass.fqNameWhenAvailable}")
                return WasmUnreachable
            }
            return WasmI32Const(klassMetadata.id)
        }

        if (function.symbol == data.backendContext.wasmSymbols.wasmInterfaceId) {
            val iface = expression.getTypeArgument(0)!!.getClass() ?: error("No interface given for wasmInterfaceId intrinsic")
            assert(iface.isInterface)
            return WasmI32Const(data.typeInfo.interfaces[iface]!!.id)
        }

        val name = data.getGlobalName(function)
        return WasmCall(name, wasmArguments)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: WasmCodegenContext): WasmInstruction {
        error("TypeOperatrs should be lowered ${expression.dump()}")
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: WasmCodegenContext): WasmInstruction {
        TODO("IrGetEnumValue")
    }

    override fun visitBlockBody(body: IrBlockBody, data: WasmCodegenContext): WasmInstruction {
        TODO()
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: WasmCodegenContext): WasmInstruction {
        assert(expression.statements.isNotEmpty()) {
            "Empty blocks are not supported in expression context"
        }
        val watStatements = expression.statements.dropLast(1).flatMap { statementToWasmInstruction(it, data) }
        val watExpression = expressionToWasmInstruction(expression.statements.last() as IrExpression, data)
        if (watStatements.isEmpty())
            return watExpression
        return WasmBlock(watStatements + listOf(watExpression), resultType = data.resultType(expression.type))
    }

    override fun visitExpression(expression: IrExpression, data: WasmCodegenContext): WasmInstruction {
        return expressionToWasmInstruction(expression, data)
    }

    override fun visitBreak(jump: IrBreak, data: WasmCodegenContext): WasmInstruction {
        return WasmBr(data.getBreakLabelName(jump.loop))
    }

    override fun visitContinue(jump: IrContinue, data: WasmCodegenContext): WasmInstruction {
        return WasmBr(data.getContinueLabelName(jump.loop))
    }

    override fun visitReturn(expression: IrReturn, data: WasmCodegenContext): WasmInstruction {
        if (expression.value.type.isUnit()) return WasmReturn(emptyList())

        return WasmReturn(listOf(expressionToWasmInstruction(expression.value, data)))
    }

    override fun visitThrow(expression: IrThrow, data: WasmCodegenContext): WasmInstruction {
        TODO("IrThrow")
    }

    override fun visitVariable(declaration: IrVariable, data: WasmCodegenContext): WasmInstruction {
        val init = declaration.initializer ?: return WasmNop
        val varName = data.getLocalName(declaration)
        return WasmSetLocal(varName, expressionToWasmInstruction(init, data))
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: WasmCodegenContext): WasmInstruction {
        if (data.currentClass!!.defaultType.isAny()) return WasmNop
        return transformCall(expression, data, WasmGetLocal("__this__"))
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: WasmCodegenContext): WasmInstruction {
        TODO("IrInstanceInitializerCall")
    }

    override fun visitTry(aTry: IrTry, data: WasmCodegenContext): WasmInstruction {
        TODO("IrTry")
    }

    override fun visitWhen(expression: IrWhen, data: WasmCodegenContext): WasmInstruction {
        val resultType = data.resultType(expression.type)
        return expression.branches.foldRight(null) { br: IrBranch, inst: WasmInstruction? ->
            val body = expressionToWasmInstruction(br.result, data)
            if (isElseBranch(br)) body
            else {
                val condition = expressionToWasmInstruction(br.condition, data)
                WasmIf(condition, resultType, WasmThen(listOf(body)), inst?.let { WasmElse(listOf(inst)) })
            }
        }!!
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: WasmCodegenContext): WasmInstruction {
        TODO("IrWhileLoop")
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: WasmCodegenContext): WasmInstruction {
        TODO("IrDoWhileLoop")
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: WasmCodegenContext): WasmInstruction {
        TODO("IrSyntheticBody")
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: WasmCodegenContext): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: WasmCodegenContext): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")
}

fun expressionToWasmInstruction(expression: IrExpression, context: WasmCodegenContext): WasmInstruction {
    return expression.accept(ExpressionTransformer(), context)
}