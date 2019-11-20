/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.decompiler.DecompileIrTreeVisitor
import org.jetbrains.kotlin.decompiler.DecompileIrTreeVisitor.Companion.obtainTypeDescription
import org.jetbrains.kotlin.decompiler.decompile
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.*
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator.*
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

val OPERATOR_TOKENS = mapOf<IrStatementOrigin, String>(
    OROR to "||",
    ANDAND to "&&",
    PLUS to "+",
    MINUS to "-",
    UPLUS to "+",
    UMINUS to "-",
    EXCL to "!",
    MUL to "*",
    DIV to "/",
    PERC to "%",
    EQ to "=",
    PLUSEQ to "+=",
    MINUSEQ to "-=",
    MULTEQ to "*=",
    DIVEQ to "/=",
    PERCEQ to "%=",
    EQEQ to "==",
    EQEQEQ to "===",
    EXCLEQ to "!=",
    EXCLEQEQ to "!==",
    GT to ">",
    LT to "<",
    GTEQ to ">=",
    LTEQ to "<=",
    ELVIS to "?:",
    SAFE_CALL to "?.",
    EXCLEXCL to "!!",
    RANGE to "..",
    PREFIX_INCR to "++",
    PREFIX_DECR to "--",
    POSTFIX_INCR to "++",
    POSTFIX_DECR to "--"
)

val OPERATOR_NAMES = setOf("less", "lessOrEqual", "greater", "greaterOrEqual", "EQEQ", "EQEQEQ")

val TYPE_OPERATOR_TOKENS = mapOf(SAFE_CAST to "as?", CAST to "as", INSTANCEOF to "is", NOT_INSTANCEOF to "!is")
const val EMPTY_TOKEN = ""
const val THIS_TOKEN = "this"
const val TRY_TOKEN = "try"
const val CATCH_TOKEN = "catch"
const val CTOR_TOKEN = "constructor"
const val INIT_TOKEN = "init"
const val FINALLY_TOKEN = "finally"
const val TYPEALIAS_TOKEN = "typealias"
const val SUPER_TOKEN = "super"
const val THROW_TOKEN = "throw"
const val CLASS_TOKEN = "class"
const val INTERFACE_TOKEN = "interface"
const val COMPANION_TOKEN = "companion"
const val OBJECT_TOKEN = "object"
const val INNER_TOKEN = "inner"
const val INLINE_TOKEN = "inline"
const val DATA_TOKEN = "data"
const val EXTERNAL_TOKEN = "external"
const val ANNOTATION_TOKEN = "annotation class"
const val ENUM_TOKEN = "enum class"
const val IF_TOKEN = "if"
const val WHEN_TOKEN = "when"
const val ELSE_TOKEN = "else"
const val DO_TOKEN = "do"
const val WHILE_TOKEN = "while"
const val RETURN_TOKEN = "return"
const val OVERRIDE_TOKEN = "override"
const val FUN_TOKEN = "fun"

internal inline fun DecompileIrTreeVisitor.withBracesLn(body: () -> Unit) {
    printer.printlnWithNoIndent(" {")
    indented(body)
    printer.println("}")
}

internal inline fun DecompileIrTreeVisitor.withBraces(body: () -> Unit) {
    printer.printlnWithNoIndent(" {")
    indented(body)
    printer.print("}")
}


internal inline fun DecompileIrTreeVisitor.indented(body: () -> Unit) {
    printer.pushIndent()
    body()
    printer.popIndent()
}

internal fun concatenateNonEmptyWithSpace(vararg flags: String?) =
    flags.filterNotNull()
        .filterNot { it.isEmpty() }
        .run {
            if (isNotEmpty())
                joinToString(separator = " ")
            else
                EMPTY_TOKEN
        }

internal fun IrDeclaration.name(): String = descriptor.name.asString()

internal fun IrTypeAlias.obtainTypeAliasFlags(): String =
    concatenateNonEmptyWithSpace(
        "actual".takeIf { isActual }
    )

internal fun IrClass.obtainModality(): String? =
    when {
        modality != Modality.FINAL && !isInterface -> "${modality.name.toLowerCase()} "
        else -> null
    }

internal fun IrProperty.obtainModality(): String? =
    when {
        modality != Modality.FINAL -> "${modality.name.toLowerCase()} "
        else -> null
    }

internal fun IrSimpleFunction.obtainModality(): String? =
    when {
        modality != Modality.FINAL -> "${modality.name.toLowerCase()} "
        else -> null
    }


internal fun IrCall.obtainUnaryOperatorCall(scopeList: List<String>): String =
    "${OPERATOR_TOKENS[origin]}(${dispatchReceiver?.decompile(scopeList).orEmpty()})"


internal fun IrCall.obtainBinaryOperatorCall(scopeList: List<String>): String =
    concatenateNonEmptyWithSpace(dispatchReceiver?.decompile(scopeList), OPERATOR_TOKENS[origin], getValueArgument(0)?.decompile(scopeList))


//TODO разобраться когда тут вызов compareTo, а когда реальное сравнение
internal fun IrCall.obtainComparisonOperatorCall(scopeList: List<String>): String {
    val leftOperand = if (dispatchReceiver != null) dispatchReceiver else getValueArgument(0)
    val rightOperand = if (dispatchReceiver != null) getValueArgument(0) else getValueArgument(1)
    return if (symbol.owner.name() in OPERATOR_NAMES) {
        concatenateNonEmptyWithSpace(leftOperand?.decompile(scopeList), OPERATOR_TOKENS[origin], rightOperand?.decompile(scopeList))
    } else {
        "${leftOperand?.decompile(scopeList)}.${symbol.owner.name()}(${rightOperand?.decompile(scopeList)})"
    }
}

internal fun IrCall.obtainNotEqCall(scopeList: List<String>): String =
    if (symbol.owner.name().toLowerCase() != "not")
        concatenateNonEmptyWithSpace(
            getValueArgument(0)?.decompile(scopeList),
            OPERATOR_TOKENS[origin],
            getValueArgument(1)?.decompile(scopeList)
        )
    else
        dispatchReceiver?.decompile(scopeList).orEmpty()

internal fun IrCall.obtainNameWithArgs(scopeList: List<String>): String {
    val typeArguments = (0 until typeArgumentsCount).map { getTypeArgument(it)?.obtainTypeDescription(scopeList) }
        .joinToString(", ", "<", ">")
        .takeIf { typeArgumentsCount > 0 } ?: EMPTY_TOKEN
    val result = symbol.owner.name() + typeArguments
    val callArgumentsWithNulls = (0 until valueArgumentsCount).map {
        getValueArgument(it)
    }
    // Проверяем, что все аргументы из декларации явно определены. 
    // Если это не так, значит есть значение по-умолчанию (null в argumentsByIndex). 
    // Будем брать по индексу it из декларации имена, конкатенируя в корректный именованный аргумент 
    return result + if (valueArgumentsCount != callArgumentsWithNulls.filterNotNull().size) {
        ArrayList<String>().apply {
            for (i in callArgumentsWithNulls.indices) {
                if (callArgumentsWithNulls[i] != null) {
                    add("${symbol.owner.valueParameters[i].name()} = ${callArgumentsWithNulls[i]?.decompile(scopeList)}")
                }
            }
        }.joinToString(separator = ", ", prefix = "(", postfix = ")")
    } else {
        // Если только 1 аргумент-лямбда, то круглые скобки можно не ставить
        if (valueArgumentsCount == 1
            && getValueArgument(0) is IrFunctionExpression
            && (getValueArgument(0) as IrFunctionExpression).origin == LAMBDA
        ) {
            // Предположим, что у нас только 1 return выражение
            getValueArgument(0)?.decompile(scopeList + "lambda")

        } else {
            (0 until valueArgumentsCount).mapNotNull {
                getValueArgument(it)?.decompile(scopeList)
            }.joinToString(separator = ", ", prefix = "(", postfix = ")")
        }
    }
}

internal fun IrCall.obtainOperatorEqCall(scopeList: List<String>): String {
    val valueArgument = getValueArgument(0)
    return when (valueArgument) {
        is IrMemberAccessExpression -> with(valueArgument as IrMemberAccessExpression) {
            val leftOperand = if (dispatchReceiver != null) dispatchReceiver else getValueArgument(0)
            val rightOperand = if (dispatchReceiver != null) getValueArgument(0) else getValueArgument(1)
            concatenateNonEmptyWithSpace(
                (leftOperand as IrCall).symbol.owner.decompile(scopeList),
                OPERATOR_TOKENS[origin],
                rightOperand?.decompile(scopeList)
            )
        }

        else -> valueArgument?.decompile(scopeList) ?: EMPTY_TOKEN
    }
}


internal fun IrCall.obtainCall(scopeList: List<String>): String {
    return when (origin) {
        UPLUS, UMINUS -> obtainUnaryOperatorCall(scopeList)
        PLUS, MINUS, MUL, DIV, PERC, ANDAND, OROR -> obtainBinaryOperatorCall(scopeList)
        // === и !==
        EQEQ, GT, LT, GTEQ, LTEQ -> obtainComparisonOperatorCall(scopeList)
        EXCLEQ -> obtainNotEqCall(scopeList)
        GET_PROPERTY -> obtainGetPropertyCall(scopeList)
        // сюда прилетает только правая часть, левая разбирается в visitSetVariable
        PLUSEQ, MINUSEQ, MULTEQ, DIVEQ -> obtainOperatorEqCall(scopeList)
        // Для присваивания свойстам в конструкторах
        EQ -> obtainEqCall(scopeList)
        EXCLEXCL -> "${getValueArgument(0)!!.decompile(scopeList)}!!"
        EXCL -> "!${dispatchReceiver?.decompile(scopeList)}"
        else -> {
            var result: String? = null
            if (dispatchReceiver != null) {
                result = if (superQualifierSymbol != null) {
                    "$SUPER_TOKEN<${superQualifierSymbol!!.owner.name()}>."
                } else {
                    "${dispatchReceiver!!.decompile(scopeList)}."
                }
            } else if (extensionReceiver != null) {
                result = "${extensionReceiver!!.decompile(scopeList)}."
            }

            "${result.orEmpty()}${obtainNameWithArgs(scopeList)}"
        }
    }
}


internal fun IrCall.obtainEqCall(scopeList: List<String>): String =
    "${dispatchReceiver?.decompile(scopeList)}.${symbol.owner.decompile(scopeList)} = ${getValueArgument(0)?.decompile(scopeList)}"

internal fun IrCall.obtainGetPropertyCall(scopeList: List<String>): String {
    val fullName = symbol.owner.name()
    val regex = """<get-(.+)>""".toRegex()
    val matchResult = regex.find(fullName)
    val propName = matchResult?.groups?.get(1)?.value
    val decompiledReceiver = dispatchReceiver?.decompile(scopeList)
    return "${if (decompiledReceiver != null) "$decompiledReceiver." else EMPTY_TOKEN}$propName"
}

internal fun IrConstructor.obtainValueParameterTypes(scopeList: List<String>): String =
    valueParameters.joinToString(separator = ", ", prefix = "(", postfix = ")") { valueParameter ->
        concatenateNonEmptyWithSpace(
            valueParameter.obtainValueParameterFlags(),
            run {
                // Если среди пропертей нет с таким именем, то у нас просто x: Int
                val mappedProp = parentAsClass.declarations.filterIsInstance<IrProperty>()
                    .filter { irProperty -> irProperty.name() == valueParameter.name() }.firstOrNull()
                return@run when {
                    mappedProp == null -> EMPTY_TOKEN
                    mappedProp.isVar -> "var"
                    else -> "val"
                }
            },
            "${valueParameter.name()}:",
            (valueParameter.varargElementType?.toKotlinType() ?: valueParameter.type.obtainTypeDescription(scopeList)).toString(),
            if (valueParameter.hasDefaultValue()) " = ${valueParameter.defaultValue!!.decompile(scopeList)}" else EMPTY_TOKEN
        )
    }.takeIf { valueParameters.size > 0 || parentAsClass.constructors.count() > 1 } ?: EMPTY_TOKEN


internal fun IrFunction.obtainValueParameterTypes(scopeList: List<String>): String =
    ArrayList<String>().apply {
        valueParameters.mapTo(this) {
            concatenateNonEmptyWithSpace(
                it.obtainValueParameterFlags(),
                "${it.name()}:",
                (it.varargElementType?.toKotlinType() ?: it.type.obtainTypeDescription(scopeList)).toString(),
                if (it.hasDefaultValue()) " = ${it.defaultValue!!.decompile(scopeList)}" else EMPTY_TOKEN
            )
        }
    }.joinToString(separator = ", ", prefix = "(", postfix = ")")

internal fun IrDeclarationWithVisibility.obtainVisibility(): String =
    when (visibility) {
        Visibilities.PUBLIC, Visibilities.LOCAL -> EMPTY_TOKEN
        else -> visibility.name.toLowerCase()
    }


internal fun IrClass.obtainInheritance(): String {
    val implementedInterfaces = superTypes
        .filter { !it.isAny() && it.toKotlinType().isInterface() }
        .map {
            it.toKotlinType().toString()
        }
    return implementedInterfaces.joinToString(", ")
}

//internal fun concatenateConditions(branch: IrBranch): String {
//    when (branch.condition) {
//        is IrIfThenElseImpl -> {
//            val irIfThenElseImplBranch = branch.condition as IrIfThenElseImpl
//            val firstBranch = irIfThenElseImplBranch.branches[0]
//            val secondBranch = irIfThenElseImplBranch.branches[1]
//            return when (irIfThenElseImplBranch.origin) {
//                OROR -> return "(" + concatenateNonEmptyWithSpace(
//                    secondBranch.result.decompile(""),
//                    OPERATOR_TOKENS[irIfThenElseImplBranch.origin],
//                    concatenateConditions(firstBranch)
//                ) + ")"
//                ANDAND -> {
//                    var result = concatenateConditions(firstBranch)
//                    if (firstBranch.result !is IrConst<*>) {
//                        result = concatenateNonEmptyWithSpace(
//                            result,
//                            OPERATOR_TOKENS[irIfThenElseImplBranch.origin],
//                            firstBranch.result.decompile("")
//                        )
//                    }
//                    "($result)"
//                }
//                else -> TODO()
//            }
//        }
//        else -> {
//            return branch.condition.decompile("")
//        }
//    }
//}

internal fun IrClass.obtainDeclarationStr(): String =
    if (name() == "<no name provided>") "object"
    else concatenateNonEmptyWithSpace(
        obtainVisibility(),
        obtainModality(),
        obtainClassFlags(),
        "${name().takeIf { !isCompanion } ?: EMPTY_TOKEN}${obtainTypeParameters()}")

internal fun IrClass.obtainClassFlags() =
    concatenateNonEmptyWithSpace(
        INTERFACE_TOKEN.takeIf { isInterface },
        COMPANION_TOKEN.takeIf { isCompanion },
        OBJECT_TOKEN.takeIf { isObject },
        INNER_TOKEN.takeIf { isInner },
        INLINE_TOKEN.takeIf { isInline },
        DATA_TOKEN.takeIf { isData },
        EXTERNAL_TOKEN.takeIf { isExternal },
        ANNOTATION_TOKEN.takeIf { isAnnotationClass },
        ENUM_TOKEN.takeIf { isEnumClass },
        CLASS_TOKEN.takeIf { isClass }
    )

internal fun IrClass.isPrimaryCtorArg(argName: String): Boolean {
    val primaryCtor = primaryConstructor
    val primaryCtorArgNames = primaryCtor?.valueParameters?.map { it.name() }
    return !primaryCtorArgNames.isNullOrEmpty() && primaryCtorArgNames.contains(argName)
}

internal fun IrVariable.obtainVariableFlags(): String =
    concatenateNonEmptyWithSpace(
        "const".takeIf { isConst },
        "lateinit".takeIf { isLateinit },
        if (isVar) "var" else "val"
    )

internal fun IrProperty.obtainPropertyFlags() =
    concatenateNonEmptyWithSpace(
        "external".takeIf { isExternal },
        "const".takeIf { isConst },
        "lateinit".takeIf { isLateinit },
        if (isVar) "var" else "val"
    )
// По идее эта штука должна быть в companion object визитора и резолвить конфликты имен

internal fun IrValueParameter.obtainValueParameterFlags(): String =
    concatenateNonEmptyWithSpace(
        "vararg".takeIf { varargElementType != null },
        "crossinline".takeIf { isCrossinline },
        "noinline".takeIf { isNoinline }
    )

internal fun IrTypeParametersContainer.obtainTypeParameters(): String =
    if (typeParameters.isEmpty())
        EMPTY_TOKEN
    else
        typeParameters.joinToString(separator = ", ", prefix = "<", postfix = ">") {
            it.obtain()
        }.trim()

private fun IrTypeParameter.variance() = variance.label
private fun IrTypeParameter.bound() =
    superTypes
        .filterNot { it.isNullableAny() }
        .map { it.toKotlinType().toString() }
        .ifNotEmpty { joinToString(", ", prefix = " : ") } ?: EMPTY_TOKEN

internal fun IrTypeParameter.obtain() =
    concatenateNonEmptyWithSpace(variance(), name(), bound())

//TODO посмотреть где это используется (особенно с IrTypeAbbreviation)
internal fun IrTypeArgument.obtain(scopeList: List<String>) =
    when (this) {
        is IrStarProjection -> "*"
        is IrTypeProjection -> "${if (variance.label.isNotEmpty()) variance.label + " " else EMPTY_TOKEN}${type.obtainTypeDescription(
            scopeList
        )}"
        else -> throw AssertionError("Unexpected IrTypeArgument: $this")
    }

internal fun IrSimpleFunction.isOverriden() =
    overriddenSymbols.isNotEmpty() && overriddenSymbols.map { it.owner.name() }.contains(name())

internal fun IrSimpleFunction.obtainSimpleFunctionFlags(): String =
    concatenateNonEmptyWithSpace(
        // TODO выпилить при появлении в ноде информации о модификаторе operator
        "operator".takeIf { descriptor.isOperator },
        "tailrec".takeIf { isTailrec },
        "inline".takeIf { isInline },
        "external".takeIf { isExternal },
        "suspend".takeIf { isSuspend }
    )

internal fun IrSimpleFunction.obtainCustomGetter() {
    when (origin) {
        IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR -> {
        }
        else -> TODO("Not yet implemented for custom getter!")
    }
}

internal fun IrSimpleFunction.obtainCustomSetter() {
    when (origin) {
        IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR -> {
        }
        else -> TODO("Not yet implemented for custom setter!")
    }
}

internal fun decompileAnnotations(element: IrAnnotationContainer) {
    //TODO правильно рендерить аннотации
}


internal fun IrFunction.obtainFunctionName(): String {
    var result = EMPTY_TOKEN
    if (extensionReceiverParameter != null) {
        result = "${extensionReceiverParameter?.type?.toKotlinType().toString()}."
    }
    return "$result${name()}"
}