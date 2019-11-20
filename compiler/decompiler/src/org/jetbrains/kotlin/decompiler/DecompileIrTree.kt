/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.decompiler.util.*
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DELEGATED_MEMBER
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.*
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator.IMPLICIT_CAST
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer

fun IrElement.decompile(data: List<String>): String =
    StringBuilder().also { sb ->
        accept(DecompileIrTreeVisitor(sb), data)
    }.toString().trimEnd()


class DecompileIrTreeVisitor(
    out: Appendable
) : IrElementVisitor<Unit, List<String>> {

    internal val printer = Printer(out, "    ")

    companion object {
        val irFileNamesToImportedDeclarationsMap = mutableMapOf<IrFile, Set<String>>()
        //TODO резолвить конфликты имен типов возвращаемых значений.
        // Конфликт - если более 2 записей, заканчивающихся на этот тип
        internal fun IrType.obtainTypeDescription(scopeList: List<String>): String {
            if ((this as? IrSimpleType)?.abbreviation != null) {
                with(abbreviation!!.typeAlias.owner) {
                    return (fqNameWhenAvailable?.asString() ?: name()) + this@obtainTypeDescription.arguments.joinToString(
                        ", ",
                        "<",
                        ">"
                    ) { it.obtain(scopeList) }
                }
            }
            return if (toKotlinType().isFunctionTypeOrSubtype) {
                val arguments = toKotlinType().arguments
                val returnType = arguments.last().type
                val inputTypes = arguments.dropLast(1)
                "${inputTypes.joinToString(", ", prefix = "(", postfix = ")") {
                    it.type.toString() + ("?".takeIf { isNullable() } ?: EMPTY_TOKEN)
                }} -> $returnType"
            } else {
                if (getClass()?.isLocalClass() == true) {
                    // Если локальный класс, то оставляем fqName без префикса родительского
                    // TODO - тут по идее может быть вложенность inner/nested/object.
                    // Надо брать fqName не от parent, а от самого близкого parent не local
                    getClass()?.fqNameWhenAvailable?.asString()?.removePrefix(
                        getClass()?.parent?.fqNameForIrSerialization?.asString()?.let { "$it." } ?: EMPTY_TOKEN
                    ) ?: EMPTY_TOKEN
//                    getClass()?.name()
//                    toKotlinType().toString().substring(startIndex = toKotlinType().toString().indexOfLast { it == '.' })
                } else {
                    //Пока так, но нужно резолвить через importStr и сравнение постфиксов
                    getClass()?.fqNameWhenAvailable?.asString() ?: EMPTY_TOKEN
                }
            }
        }

    }

    override fun visitFile(declaration: IrFile, data: List<String>) {
        with(declaration) {
            val importResolveVisitor = ImportResolveVisitor()
            val scopeList = listOfNotNull(fqName.asString().takeIf { declaration.fqName != FqName.ROOT })
            accept(importResolveVisitor, scopeList)
            irFileNamesToImportedDeclarationsMap[this] = importResolveVisitor.importDirectivesSet
            printer.println(declaration.declarations.joinToString(separator = "\n", postfix = "\n") { it.decompile(scopeList) })
        }
    }


    override fun visitGetObjectValue(expression: IrGetObjectValue, data: List<String>) {
        with(expression) {
            val owner = symbol.owner
            if (owner.isCompanion) printer.printWithNoIndent(owner.parentAsClass.name())
            else printer.printWithNoIndent(expression.type.obtainTypeDescription(data))
        }
    }


    override fun visitBlockBody(body: IrBlockBody, data: List<String>) {
        withBracesLn {
            body.statements
                //Вызовы родительского конструктора, в каких случаях явно оставлять?
                .filterNot { it is IrDelegatingConstructorCall }
                .filterNot { it is IrInstanceInitializerCall }
                .decompileElements(data)
        }
    }


    override fun visitReturn(expression: IrReturn, data: List<String>) {
        when {
            expression.value is IrBlock && (expression.value as IrBlock).origin != OBJECT_LITERAL -> {
                printer.println((expression.value as IrBlock).statements[0].decompile(data + "return"))
                printer.println("$RETURN_TOKEN ${(expression.value as IrBlock).statements[1].decompile(data + "return")}")
            }
            data.last() == "lambda" -> expression.value.accept(this, data)
            else -> printer.println("$RETURN_TOKEN ${expression.value.decompile(data + "return")}")
        }
    }


    override fun visitClass(declaration: IrClass, data: List<String>) {
        val scopeList = data + declaration.name()
        printer.print(declaration.obtainDeclarationStr())
        when (declaration.kind) {
            ClassKind.CLASS, ClassKind.OBJECT -> {
                declaration.obtainPrimaryCtorWithInheritance(scopeList)
                withBracesLn {
                    val secondaryCtors = declaration.constructors.filterNot { it.isPrimary }
                    secondaryCtors.forEach {
                        it.obtainSecondaryCtor(scopeList)
                    }

                    declaration.declarations
                        .filterNot { it is IrConstructor || it is IrField }
                        .filterNot {
                            it.origin in setOf(
                                IrDeclarationOrigin.FAKE_OVERRIDE,
                                IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER
                            )
                        }
                        .forEach { it.accept(this, scopeList) }
                }
            }
            ClassKind.ANNOTATION_CLASS -> {
                withBracesLn {
                    declaration.declarations
                        .filterNot { it is IrConstructor }
                        .filterNot {
                            it.origin == IrDeclarationOrigin.FAKE_OVERRIDE
                        }
                        .decompileElements(scopeList)
                }
            }
            ClassKind.ENUM_CLASS -> {
                declaration.obtainPrimaryCtorWithInheritance(scopeList)
                withBracesLn {
                    printer.println(declaration.declarations
                                        .filterIsInstance<IrEnumEntry>()
                                        .joinToString(", ", postfix = ";") { it.decompile(scopeList) }
                    )
                }
            }
            else -> {
                val implStr = declaration.obtainInheritanceWithDelegation(scopeList)
                if (implStr.isNotEmpty()) {
                    printer.printWithNoIndent(": $implStr")
                }
                withBracesLn {
                    declaration.declarations
                        .filterNot { it is IrConstructor || it is IrFieldAccessExpression }
                        .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                        .decompileElements(scopeList)
                }
            }
        }
    }

    private fun IrClass.obtainPrimaryCtorWithInheritance(scopeList: List<String>) {
        if (primaryConstructor == null) {
            printer.printWithNoIndent(
                " : ${obtainInheritanceWithDelegation(scopeList)}"
                    .takeIf { superTypes.isNotEmpty() } ?: EMPTY_TOKEN
            )

        } else {
            if (!isObject && !isCompanion) {
                printer.printWithNoIndent(primaryConstructor!!.obtainValueParameterTypes(scopeList))
            }
            with(primaryConstructor!!) {
                val delegatingCtorCall = body!!.statements.filterIsInstance<IrDelegatingConstructorCall>().firstOrNull()
                //TODO чтобы не дублировался вызов ктора и перечисления через запятую (делегирование)
                superTypes.remove(delegatingCtorCall?.symbol?.owner?.constructedClassType)
                val implStr = parentAsClass.obtainInheritanceWithDelegation(scopeList)
                val delegatingCtorCallStr = delegatingCtorCall?.decompile(scopeList) ?: ""
                if (delegatingCtorCallStr.isEmpty()) {
                    printer.printWithNoIndent(": $implStr".takeIf { implStr.isNotEmpty() }.orEmpty())
                } else {
                    printer.printWithNoIndent("$delegatingCtorCallStr${", $implStr".takeIf { implStr.isNotEmpty() }.orEmpty()}")
                }

            }
        }
    }

    private fun IrClass.obtainInheritanceWithDelegation(data: List<String>): String {
        val delegatedMap = mutableMapOf<IrType, IrExpression>()
        declarations.filterIsInstance<IrField>().forEach {
            val delegationFieldInitializer = it.initializer!!.expression
            delegatedMap[delegationFieldInitializer.type] = delegationFieldInitializer
        }
        // Для енамов в суперах лежит Enum<MyType>, который почему-то не isEnum(
        return superTypes
            .filterNot { it.isAny() || it.toKotlinType().toString().startsWith("Enum") }
            .filterNot { primaryConstructor?.constructedClass?.symbol?.let { it1 -> it.isSubtypeOfClass(it1) } ?: false }
            .joinToString(", ") {
                val result = it.obtainTypeDescription(data)
                val key = delegatedMap.keys.filter { it.isSubtypeOfClass(it.getClass()!!.symbol) }.firstOrNull()
                result + if (key != null) " by ${delegatedMap[key]?.decompile(data)}" else EMPTY_TOKEN
            }

    }

    private fun IrConstructor.obtainSecondaryCtor(scopeList: List<String>) {
        printer.print("${obtainVisibility()}constructor${obtainValueParameterTypes(scopeList)}")
        val delegatingCtorCall = body!!.statements.filterIsInstance<IrDelegatingConstructorCall>().first()
        val delegatingCtorClass = delegatingCtorCall.symbol.owner.returnType
        printer.printWithNoIndent(" : ${delegatingCtorCall.renderDelegatingIntoSecondary(returnType != delegatingCtorClass, scopeList)}")
        val implStr = parentAsClass.obtainInheritance()
        printer.printWithNoIndent(", $implStr".takeIf { implStr.isNotEmpty() }.orEmpty())
        body?.accept(this@DecompileIrTreeVisitor, scopeList)
    }

    private fun IrDelegatingConstructorCall.renderDelegatingIntoSecondary(isSuper: Boolean, scopeList: List<String>): String {
        var result = if (isSuper) SUPER_TOKEN else THIS_TOKEN
        result += (0 until valueArgumentsCount).map { getValueArgument(it)?.decompile(scopeList) }
            .joinToString(", ", "(", ")")
        return result
    }


    //TODO TypeParameters (дженерики)
    override fun visitTypeAlias(declaration: IrTypeAlias, data: List<String>) {
        with(declaration) {
            printer.println(
                concatenateNonEmptyWithSpace(
                    obtainTypeAliasFlags(),
                    obtainVisibility(),
                    TYPEALIAS_TOKEN,
                    name(),
                    OPERATOR_TOKENS[EQ],
                    expandedType.obtainTypeDescription(data)
                )
            )
        }
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: List<String>) {
        body.expression.accept(this, data)
    }


    //TODO А когда тут DEFAULT_PROPERTY_ACCESSOR и что с ним делать?
    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: List<String>) {
        val scopeList = data + declaration.name()
        with(declaration) {
            when {
                origin != DEFAULT_PROPERTY_ACCESSOR && origin != DELEGATED_MEMBER -> {
                    printer.print(
                        concatenateNonEmptyWithSpace(
                            obtainSimpleFunctionFlags(),
                            OVERRIDE_TOKEN.takeIf { isOverriden() },
                            obtainModality().takeIf { !isOverriden() },
                            obtainVisibility(),
                            FUN_TOKEN,
                            obtainTypeParameters(),
                            "${obtainFunctionName()}${obtainValueParameterTypes(data)}",
                            if (!returnType.isUnit()) ": ${returnType.obtainTypeDescription(scopeList)} " else EMPTY_TOKEN
                        )
                    )
                    declaration.body?.accept(this@DecompileIrTreeVisitor, scopeList)
                    printer.printlnWithNoIndent()
                }
                origin != DELEGATED_MEMBER -> {
                    printer.printWithNoIndent(declaration.correspondingPropertySymbol!!.owner.name())
                }
                else -> {
                }
            }

        }
    }

    //Пока заглушил вывод типов для variable, но надо обсудить с Димой, когда они реально нужны
    override fun visitVariable(declaration: IrVariable, data: List<String>) {
        printer.println(
            with(declaration) {
                when {
                    origin == IrDeclarationOrigin.CATCH_PARAMETER -> concatenateNonEmptyWithSpace(
                        name(),
                        ":",
                        type.obtainTypeDescription(data)
                    )
                    initializer is IrBlock -> {
                        if ((initializer as IrBlock).origin == OBJECT_LITERAL) {
                            concatenateNonEmptyWithSpace(
                                obtainVariableFlags(),
                                name(),
                                "=",
                                initializer?.decompile(data)
                            )
                        } else {
                            val variableDeclarations = (initializer as IrBlock).statements.filterIsInstance<IrVariable>()
                            variableDeclarations.forEach { it.accept(this@DecompileIrTreeVisitor, data) }
                            val lastStatement = (initializer as IrBlock).statements.last()
                            concatenateNonEmptyWithSpace(
                                obtainVariableFlags(),
                                name(),
                                ":",
                                type.obtainTypeDescription(data),
                                "=",
                                lastStatement.decompile(data)
                            )
                        }
                    }
                    else -> concatenateNonEmptyWithSpace(
                        obtainVariableFlags(),
                        name(),
                        ":",
                        type.obtainTypeDescription(data),
                        "=",
                        initializer?.decompile(data)
                    )
                }
            }
        )
    }

    override fun visitProperty(declaration: IrProperty, data: List<String>) {
        with(declaration) {
            if ((parent !is IrClass || !parentAsClass.isPrimaryCtorArg(name())) && origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                var result = concatenateNonEmptyWithSpace(obtainVisibility(), obtainModality(), obtainPropertyFlags())
                if (backingField != null) {
                    result =
                        concatenateNonEmptyWithSpace(
                            result,
                            declaration.name()
//                            backingField!!.name()
                            //TODO заглушил, т.к. для делегированных пропертей информация выводится некорректно (operator getValue)
//                            ":",
//                            backingField!!.type.obtainTypeDescription()
                        )
                    if (backingField!!.initializer != null) {
                        if (parent is IrClass && parentAsClass.kind == ClassKind.OBJECT) {
                            result =
                                concatenateNonEmptyWithSpace(
                                    result,
                                    "=",
                                    backingField!!.initializer!!.decompile(data)
                                )
                        } else {
                            result = concatenateNonEmptyWithSpace(
                                result,
                                "by".takeIf { isDelegated } ?: "=",
                                backingField!!.initializer!!.decompile(data))
                        }
                    }
                } else {
                    result = concatenateNonEmptyWithSpace(
                        result,
                        name(),
                        getter?.returnType?.obtainTypeDescription(data)?.let { ": $it" } ?: EMPTY_TOKEN
                    )
                }
                printer.println(result)
//                getter?.obtainCustomGetter()
//                setter?.obtainCustomSetter()
            }
        }
    }

    //TODO когда это используется, вроде все разбираем на уровне проперти?
    override fun visitField(declaration: IrField, data: List<String>) {
        var result =
            concatenateNonEmptyWithSpace(declaration.name(), ":", declaration.type.obtainTypeDescription(data))
        if (declaration.initializer != null) {
            result = concatenateNonEmptyWithSpace(result, "=", declaration.initializer!!.decompile(data))
        }
        printer.println(result)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: List<String>) {
        if (!expression.symbol.owner.returnType.isAny()) {
            printer.println(listOf(" : ", expression.symbol.owner.returnType.obtainTypeDescription(data),
                                   ((0 until expression.valueArgumentsCount)
                                       .mapNotNull { expression.getValueArgument(it)?.decompile(data) }
                                       .joinToString(", ", "(", ")"))).joinToString(""))
        }
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: List<String>) {
        printer.println(expression.decompile(data))
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: List<String>) {
        printer.print(INIT_TOKEN)
        declaration.body.accept(this, data)
    }

    override fun visitWhen(expression: IrWhen, data: List<String>) {
        if (expression is IrIfThenElseImpl) {
            when (expression.origin) {
                OROR -> printer.printWithNoIndent(
                    concatenateNonEmptyWithSpace(
                        expression.branches[0].condition.decompile(data),
                        OPERATOR_TOKENS[expression.origin],
                        expression.branches[1].result.decompile(data)
                    )
                )

                ANDAND -> printer.printWithNoIndent(
                    "(" + concatenateNonEmptyWithSpace(
                        expression.branches[0].condition.decompile(data),
                        OPERATOR_TOKENS[expression.origin],
                        expression.branches[0].result.decompile(data)
                    ) + ")"
                )
                else -> {
                    if (data.last() == "return") {
                        printer.printWithNoIndent("$IF_TOKEN  (${expression.branches[0].decompile(data)})")
                    } else {
                        printer.print("$IF_TOKEN  (${expression.branches[0].decompile(data)})")
                    }
                    indented {
                        withBraces {
                            printer.println(expression.branches[0].result.decompile(data))
                        }
                        if (expression.branches.size == 2) {
                            printer.printWithNoIndent(" $ELSE_TOKEN")
                            withBracesLn {
                                printer.println(expression.branches[1].result.decompile(data))
                            }
                        } else {
                            printer.println()
                        }
                    }

                }
            }
        } else {
            if (data.last() == "return") {
                printer.printWithNoIndent(WHEN_TOKEN)
            } else {
                printer.print(WHEN_TOKEN)
            }
            withBracesLn {
                expression.branches.forEach {
                    if (it is IrElseBranch) {
                        printer.print("else -> ")
                    } else {
                        printer.print(" (${it.condition.decompile(data)}) ->")
                    }
                    withBracesLn {
                        printer.println(it.result.decompile(data))
                    }
                }
            }
        }
    }

    //TODO навести тут порядок, как-то все вразнобой
    override fun visitBranch(branch: IrBranch, data: List<String>) {
        printer.print(branch.condition.decompile(data))
    }

    private fun collectCommaArguments(condition: IrExpression): String = TODO()

    override fun visitElseBranch(branch: IrElseBranch, data: List<String>) {
        printer.print("$ELSE_TOKEN -> ")
        withBracesLn {
            printer.println(branch.result.decompile(data))
        }
    }

    override fun visitSetVariable(expression: IrSetVariable, data: List<String>) {
        printer.println(
            concatenateNonEmptyWithSpace(
                expression.symbol.owner.name(),
                when (expression.origin) {
                    PLUSEQ -> " += "
                    MINUSEQ -> " -= "
                    MULTEQ -> " *= "
                    DIVEQ -> " /= "
                    else -> " = "
                },
                expression.value.decompile(data)
            )
        )
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: List<String>) {
        printer.println(expression.arguments.joinToString("", "\"", "\"") {
            when (it) {
                is IrDeclarationReference -> "$" + "{" + it.decompile(data) + "}"
                is IrConst<*> -> it.value?.toString().orEmpty()
                else -> it.decompile(data)
            }
        })
    }


    override fun visitCall(expression: IrCall, data: List<String>) {
        printer.println(expression.obtainCall(data))
    }

    override fun <T> visitConst(expression: IrConst<T>, data: List<String>) {
        if (data.last() == "return") {
            printer.printlnWithNoIndent(expression.obtainConstValue())
        } else {
            printer.println(expression.obtainConstValue())
        }
    }

    private fun <T> IrConst<T>.obtainConstValue(): String =
        when (kind) {
            IrConstKind.String -> "\"${value.toString()}\""
            IrConstKind.Char -> "\'${value.toString()}\'"
            IrConstKind.Null -> "null"
            else -> value.toString()
        }


    override fun visitGetValue(expression: IrGetValue, data: List<String>) {
        if (data.last() == "return") {
            printer.printlnWithNoIndent(expression.obtainGetValue())
        } else {
            printer.println(expression.obtainGetValue())
        }
    }

    private fun IrGetValue.obtainGetValue(): String {
        return when {
            origin == INITIALIZE_PROPERTY_FROM_PARAMETER -> EMPTY_TOKEN
            symbol.owner.name() == "<this>" || symbol.owner.origin == IrDeclarationOrigin.INSTANCE_RECEIVER -> THIS_TOKEN
            else -> symbol.owner.name()
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: List<String>) {
        var result = ("${expression.dispatchReceiver?.decompile(data)}.".takeIf { expression.dispatchReceiver != null }
            ?: EMPTY_TOKEN) + expression.type.obtainTypeDescription(data)
//        var irConstructor = expression.symbol.owner
        result += (0 until expression.typeArgumentsCount).mapNotNull {
            expression.getTypeArgument(it)?.obtainTypeDescription(data)
        }
            .joinToString(", ")
            .takeIf { expression.typeArgumentsCount > 0 } ?: EMPTY_TOKEN
        //TODO добавить проверку наличия defaultValue и именнованных вызовов
        result += (0 until expression.valueArgumentsCount)
            .mapNotNull { expression.getValueArgument(it)?.decompile(data) }
            .joinToString(", ", "(", ")")
        printer.println(result)
    }

    override fun visitGetField(expression: IrGetField, data: List<String>) {
        printer.println("${expression.receiver?.decompile(data)}.".takeIf { expression.receiver != null } ?: EMPTY_TOKEN
        + expression.symbol.owner.name())
    }

    override fun visitSetField(expression: IrSetField, data: List<String>) {
        val initValue = expression.value.decompile(data)
        val receiverValue = expression.receiver?.decompile(data) ?: ""
        val backFieldSymbolVal = expression.symbol.owner.name()
        printer.println("$receiverValue.$backFieldSymbolVal = $initValue")
    }


    override fun visitWhileLoop(loop: IrWhileLoop, data: List<String>) {
        printer.print("$WHILE_TOKEN (${loop.condition.decompile(data)})")
        withBracesLn {
            loop.body?.accept(this, data)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: List<String>) {
        printer.print(DO_TOKEN)
        withBraces {
            loop.body?.accept(this@DecompileIrTreeVisitor, data)
        }
        printer.printlnWithNoIndent("$WHILE_TOKEN (${loop.condition.decompile(data)})")
    }

    override fun visitThrow(expression: IrThrow, data: List<String>) {
        printer.println("$THROW_TOKEN ${expression.value.decompile(data)}")
    }


    override fun visitTry(aTry: IrTry, data: List<String>) {
        if (data.last() == RETURN_TOKEN) {
            printer.printWithNoIndent(TRY_TOKEN)
        } else {
            printer.print(TRY_TOKEN)
        }
        withBracesLn {
            aTry.tryResult.accept(this, data)
        }
        aTry.catches.decompileElements(data)
        if (aTry.finallyExpression != null) {
            printer.print("$FINALLY_TOKEN ")
            withBracesLn {
                aTry.finallyExpression!!.accept(this, data)
            }
        }
    }

    override fun visitCatch(aCatch: IrCatch, data: List<String>) {
        printer.print("$CATCH_TOKEN (${aCatch.catchParameter.decompile(data)}) ")
        withBracesLn {
            aCatch.result.accept(this, data)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: List<String>) {
        with(expression) {
            when (operator) {
                in TYPE_OPERATOR_TOKENS.keys -> printer.print(
                    "(${argument.decompile(data)} ${TYPE_OPERATOR_TOKENS[operator]} ${typeOperand.obtainTypeDescription(
                        data
                    )})"
                )
                IMPLICIT_COERCION_TO_UNIT, IMPLICIT_CAST -> printer.println(argument.decompile(data))
                else -> TODO("Unexpected type operator $operator!")
            }
        }
    }

    override fun visitBreak(jump: IrBreak, data: List<String>) {
        //TODO отрисовать label break@loop если label не пуст
        printer.println("break")
    }

    override fun visitContinue(jump: IrContinue, data: List<String>) {
        //TODO отрисовать label continue@loop если label не пуст
        printer.println("continue")
    }

    override fun visitVararg(expression: IrVararg, data: List<String>) {
        printer.print(expression.elements.joinToString(", ") { it.decompile(data) })
    }

    override fun visitBody(body: IrBody, data: List<String>) {
        body.acceptChildren(this, data)
    }

    override fun visitBlock(expression: IrBlock, data: List<String>) {
        if (expression.origin == OBJECT_LITERAL) {
            printer.println(expression.statements[0].decompile(data))
        } else {
            expression.acceptChildren(this, data)
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: List<String>) {
        declaration.files.forEach {
            printer.printlnWithNoIndent("// FILE: ${it.path}")
            // Чтобы соблюсти очередность package -> imports -> declarations вынуждено вытащил это из visitFile
            if (it.fqName != FqName.ROOT) {
                printer.println("package ${it.fqName.asString()}\n")
            }

            val fileSources = it.decompile(data)
            printer.println(irFileNamesToImportedDeclarationsMap[it]?.joinToString(separator = "\n", postfix = "\n") { "import $it" })
            printer.println(fileSources)
            printer.println()
        }
    }


    override fun visitComposite(expression: IrComposite, data: List<String>) {
        expression.statements.decompileElements(data)
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: List<String>) {
        printer.print("*${spread.expression.decompile(data)}")
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: List<String>) {
        printer.println(
            (0 until expression.valueArgumentsCount)
                .map { expression.getValueArgument(it)?.decompile(data) }
                .joinToString(",", "(", ")")
                .takeIf { expression.valueArgumentsCount > 0 } ?: EMPTY_TOKEN
        )
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: List<String>) {
        printer.printWithNoIndent("${expression.type.obtainTypeDescription(data)}.${expression.symbol.owner.name()}")
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: List<String>) {
        printer.print("${declaration.name()}${declaration.initializerExpression?.decompile(data)}")
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: List<String>) {
        //TODO умеем пока только сслыки на top-level функции из текущего скоупа
        printer.printWithNoIndent("::${expression.symbol.owner.name()}")
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: List<String>) {
        if (expression.origin == LAMBDA) {
            indented {
                val body = expression.function.body
                if (body?.statements?.size == 1) {
                    val firstStatement = body.statements[0]
                    if (firstStatement is IrReturn) {
                        val returnStatementCall = (firstStatement as IrReturnImpl).value as IrMemberAccessExpression
                        val leftFromArrowParams = (0 until returnStatementCall.valueArgumentsCount)
                            .map { returnStatementCall.getValueArgument(it)?.decompile(data) }
                            .toHashSet()
                            .apply { add(returnStatementCall.dispatchReceiver?.decompile(data)) }
                            .filterNotNull()
                        if (leftFromArrowParams.isNotEmpty()) {
                            printer.printWithNoIndent(" { ${leftFromArrowParams.joinToString(", ")}  -> ${firstStatement.decompile(data + "lambda")} }")
                        } else {
                            printer.printlnWithNoIndent(" { ${firstStatement.decompile(data + "lambda")} }")
                        }
                    }
                } else {
                    body?.accept(this, data)
                }
            }
        } else {
            TODO("Visitor implemented only for lambdas!")
        }

    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: List<String>) = TODO()

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: List<String>) = TODO()

    override fun visitTypeParameter(declaration: IrTypeParameter, data: List<String>) = TODO()

    //TODO если его заменяют obtainPrimaryCtor и obtainSecondaryCtor - выпилить
    override fun visitConstructor(declaration: IrConstructor, data: List<String>) = TODO()

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: List<String>) = TODO()

    override fun visitElement(element: IrElement, data: List<String>) = TODO()

    override fun visitClassReference(expression: IrClassReference, data: List<String>) {
        printer.print("${expression.type.obtainTypeDescription(data)}::class")
    }

    private fun List<IrElement>.decompileElements(data: List<String>) {
        forEach { it.accept(this@DecompileIrTreeVisitor, data) }
    }


}

