/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler

import org.jetbrains.kotlin.decompiler.util.DecompilerIrElementVisitor
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlin.utils.Printer

fun IrElement.decompile(): String =
    StringBuilder().also { sb ->
        accept(DecompileIrTreeVisitor(sb), "")
    }.toString()

fun IrFile.decompileTreesFromLineNumber(lineNumber: Int): String {
    val sb = StringBuilder()
    accept(DecompileTreeFromSourceLineVisitor(fileEntry, lineNumber, sb), null)
    return sb.toString()
}

const val disjunctionToken = "||"
const val conjunctionToken = "&&"


class DecompileIrTreeVisitor(
    out: Appendable
) : IrElementVisitor<Unit, String> {

    private val printer = Printer(out, "    ")
    private val elementDecompiler = DecompilerIrElementVisitor()
    private fun IrType.render() = elementDecompiler.renderType(this)

    override fun visitElement(element: IrElement, data: String) {
        element.generatesSourcesForElement {
            if (element is IrAnnotationContainer) {
                decompileAnnotations(element)
            }
            element.acceptChildren(this@DecompileIrTreeVisitor, "")
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: String) {
        declaration.files.decompileElements()
    }

    override fun visitFile(declaration: IrFile, data: String) {
        declaration.declarations.decompileElements()
    }

    override fun visitBlockBody(body: IrBlockBody, data: String) {
        withBraces {
            body.statements
                //Вызовы родительского конструктора, в каких случаях явно оставлять?
                .filterNot { it is IrDelegatingConstructorCall }
                .filterNot { it is IrInstanceInitializerCall }
                .decompileElements()
        }
    }

    override fun visitBody(body: IrBody, data: String) {
        body.statements.decompileElements()
    }

    override fun visitBlock(expression: IrBlock, data: String) {
        expression.statements.decompileElements()
    }


    override fun visitReturn(expression: IrReturn, data: String) {
        printer.print("${expression.accept(elementDecompiler, null)} ")
        expression.value.accept(this, "return")
    }

    private fun IrElement.decompile(): String {
        return accept(elementDecompiler, null)
    }

    override fun visitClass(declaration: IrClass, data: String) {
        printer.print(declaration.decompile())
        val implStr = declaration.renderInheritance()
        if (declaration.kind == ClassKind.CLASS) {
            declaration.primaryConstructor?.renderPrimaryCtor()
            withBraces {
                declaration.declarations
                    .filter { it is IrProperty }
                    .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                    .decompileElements()

                val secondaryCtors = declaration.constructors.filter { !it.isPrimary }
                secondaryCtors.forEach {
                    it.renderSecondaryCtor()
                }
                declaration.declarations
                    .filterNot { it is IrConstructor || it is IrProperty }
                    .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                    .decompileElements()
            }
        } else {
            if (implStr.isNotEmpty()) {
                printer.printWithNoIndent(", $implStr")
            }
            withBraces {
                declaration.declarations
                    .filterNot { it is IrConstructor }
                    .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                    .decompileElements()
            }
        }
    }

    private fun IrConstructor.renderPrimaryCtor() {
        printer.printWithNoIndent(renderValueParameterTypes())
        val delegatingCtorCall = body!!.statements.filter { it is IrDelegatingConstructorCall }.first()
        printer.printWithNoIndent(delegatingCtorCall.decompile())
        val implStr = parentAsClass.renderInheritance()
        if (implStr.isNotEmpty()) {
            printer.printWithNoIndent(", $implStr")
        }
    }

    private fun IrConstructor.renderSecondaryCtor() {
        printer.print("${renderVisibility()}constructor${renderValueParameterTypes()}")
        val delegatingCtorCall = body!!.statements.filter { it is IrDelegatingConstructorCall }.first()
        val delegatingCtorClass = (delegatingCtorCall as IrDelegatingConstructorCall).symbol.owner.returnType
        printer.printWithNoIndent(" : ${delegatingCtorCall.renderDelegatingIntoSecondary(returnType != delegatingCtorClass)}")
        val implStr = parentAsClass.renderInheritance()
        if (implStr.isNotEmpty()) {
            printer.printWithNoIndent(", $implStr")
        }
        body?.accept(this@DecompileIrTreeVisitor, "")
    }

    private fun IrDelegatingConstructorCall.renderDelegatingIntoSecondary(isSuper: Boolean): String {
        var result = ""
        result += if (isSuper) {
            "super"
        } else {
            "this"
        }
        result += (0 until valueArgumentsCount).map { getValueArgument(it)?.decompile() }
            .joinToString(", ", "(", ")")
        return result
    }


    private fun IrFunction.renderValueParameterTypes(): String =
        ArrayList<String>().apply {
            valueParameters.mapTo(this) {
                var argDefinition = "${it.name}: ${it.type.toKotlinType()}"
                if (it.defaultValue != null) {
                    argDefinition += " = ${it.defaultValue!!.decompile()}"
                }
                argDefinition
            }
        }.joinToString(separator = ", ", prefix = "(", postfix = ")")

    private inline fun IrDeclarationWithVisibility.renderVisibility(): String =
        when (visibility) {
            Visibilities.PUBLIC -> ""
            else -> "${visibility.name.toLowerCase()} "
        }


    private fun IrClass.renderInheritance(): String {
        val implementedInterfaces = superTypes
            .filter { !it.isAny() && it.toKotlinType().isInterface() }
            .map {
                it.toKotlinType().toString()
            }
        return implementedInterfaces.joinToString(", ")
    }


    override fun visitConstructor(declaration: IrConstructor, data: String) {
        printer.print(declaration.decompile())
        declaration.body?.accept(this, "")
    }


    override fun visitTypeAlias(declaration: IrTypeAlias, data: String) {
        printer.println(declaration.decompile())
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: String) = TODO()

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
        if (declaration.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
            declaration.generatesSourcesForElement {
                decompileAnnotations(declaration)
                declaration.body?.accept(this, "")
            }
            printer.printlnWithNoIndent()
        }
    }

    private fun decompileAnnotations(element: IrAnnotationContainer) {
        //TODO правильно рендерить аннотации
    }

    override fun visitVariable(declaration: IrVariable, data: String) {
        printer.println(declaration.decompile())
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        if (!declaration.isPrimaryCtorArg()) {
            printer.println(declaration.decompile())
            declaration.getter?.accept(this, "")
            declaration.setter?.accept(this, "")
        }
    }

    private fun IrProperty.isPrimaryCtorArg(): Boolean {
        val primaryCtor = parentAsClass.primaryConstructor
        val primaryCtorArgNames = primaryCtor?.valueParameters?.map { it.name }
        return !primaryCtorArgNames.isNullOrEmpty() && primaryCtorArgNames.contains(name)
    }

    override fun visitField(declaration: IrField, data: String) {
        printer.println(declaration.decompile())
    }

    private fun List<IrElement>.decompileElements() {
        forEach { it.accept(this@DecompileIrTreeVisitor, "") }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: String) {
        printer.println(expression.decompile())
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: String) {
        printer.println(expression.decompile())
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: String) {
        printer.print("init")
        declaration.body.accept(this, "")
    }

    override fun visitWhen(expression: IrWhen, data: String) {
        if (expression is IrIfThenElseImpl) {
            when (expression.origin) {
                IrStatementOrigin.IF -> {
                    printer.print("if (${concatenateConditions(expression.branches[0].condition)})")
                    withBraces {
                        expression.branches[0].result.accept(this, "")
                    }
                    if (expression.branches.size == 2) {
                        printer.print("else")
                        withBraces {
                            expression.branches[1].result.accept(this, "")
                        }
                    }
                }
                else -> TODO()
            }
        } else {
            printer.print("when")
            withBraces {
                expression.branches.decompileElements()
            }
        }
    }

    private fun concatenateConditions(condition: IrExpression): String {
        var result = ""
        when (condition) {
            is IrIfThenElseImpl -> {
                val firstBranch = condition.branches[0]
                result += "(${concatenateConditions(firstBranch.condition)})"
                if (firstBranch.result !is IrConst<*>) {
                    when (condition.origin) {
                        IrStatementOrigin.ANDAND -> {
                            result += " $conjunctionToken "
                            result += "(${concatenateConditions(firstBranch.result)})"
                        }
                        IrStatementOrigin.OROR -> {
                            result += " $disjunctionToken "
                            result += "(${concatenateConditions(firstBranch.result)})"
                        }
                        else -> {
                            TODO()
                        }
                    }
                }
            }
            is IrCallImpl -> {
                return condition.decompile()
            }
        }
        return result
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        if (branch.condition is IrIfThenElseImpl) {
            printer.print(collectCommaArguments(branch.condition))
        } else {
            printer.print(branch.condition.decompile())
        }
        printer.printWithNoIndent(" -> ")
        withBraces {
            branch.result.accept(this@DecompileIrTreeVisitor, "")
        }
    }

    private fun collectCommaArguments(condition: IrExpression): String {
        var result = ""
        when (condition) {
            is IrIfThenElseImpl -> {
                val firstBranch = condition.branches[0]
                result += "(${collectCommaArguments(firstBranch.condition)})"
                if (firstBranch.result !is IrConst<*>) {
                    result += " $disjunctionToken (${collectCommaArguments(firstBranch.result)})"
                }
                return result;
            }
            is IrCallImpl -> {
                return condition.decompile()
            }
            else -> TODO()
        }
    }

    override fun visitElseBranch(branch: IrElseBranch, data: String) {
        printer.print("else -> ")
        withBraces {
            branch.result.accept(this, "")
        }

    }

    override fun visitSetVariable(expression: IrSetVariable, data: String) {
        printer.println(expression.decompile())
    }

    override fun visitCall(expression: IrCall, data: String) {
        if (data == "return") {
            printer.printlnWithNoIndent(expression.decompile())
        } else {
            printer.println(expression.decompile())
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: String) {
        if (data == "return") {
            printer.printlnWithNoIndent(expression.decompile())
        } else {
            printer.println(expression.decompile())
        }
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) = TODO()
    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) = TODO()
    override fun visitGetValue(expression: IrGetValue, data: String) {
        if (data == "return") {
            printer.printlnWithNoIndent(expression.decompile())
        } else {
            printer.printWithNoIndent(expression.decompile())
        }
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: String) = TODO()
    override fun visitConstructorCall(expression: IrConstructorCall, data: String) = TODO()
    override fun visitGetField(expression: IrGetField, data: String) {
        TODO()
    }

    override fun visitSetField(expression: IrSetField, data: String) {
        val initValue = expression.value.decompile()
        val receiverValue = expression.receiver?.decompile() ?: ""
        val backFieldSymbolVal = expression.symbol.owner.name()
        printer.println("$receiverValue.$backFieldSymbolVal = $initValue")
    }


    override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
        loop.generatesSourcesForElement {
            withBraces {
                loop.body?.accept(this@DecompileIrTreeVisitor, "")
            }
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
        printer.println("do {\n")
        indented {
            loop.body?.accept(this@DecompileIrTreeVisitor, "")
        }
        printer.println("} ${loop.decompile()}")
    }

    override fun visitComposite(expression: IrComposite, data: String) {
        expression.statements.decompileElements()
    }

    override fun visitThrow(expression: IrThrow, data: String) {
        printer.println("throw ${expression.value.decompile()}")
    }


    override fun visitTry(aTry: IrTry, data: String) {
        if (data == "return") {
            printer.printWithNoIndent("try ")
        } else {
            printer.print("try ")
        }
        withBraces {
            aTry.tryResult.accept(this, "")
        }
        aTry.catches.decompileElements()
        if (aTry.finallyExpression != null) {
            printer.print("finally ")
            withBraces {
                aTry.finallyExpression!!.accept(this, "")
            }
        }
    }

    override fun visitCatch(aCatch: IrCatch, data: String) {
        printer.print("catch (${aCatch.catchParameter.decompile()}) ")
        withBraces {
            aCatch.result.accept(this, "")
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) {
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> expression.argument.accept(this, "")
            else -> TODO()
        }
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: String) = TODO()

    private inline fun IrElement.generatesSourcesForElement(body: () -> Unit) {
        printer.print("${decompile()} ")
        body()
    }

    private inline fun withBraces(body: () -> Unit) {
        printer.printlnWithNoIndent(" {")
        indented(body)
        printer.println("} ")
    }

    private inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }
}

class DecompileTreeFromSourceLineVisitor(
    val fileEntry: SourceManager.FileEntry,
    private val lineNumber: Int,
    out: Appendable
) : IrElementVisitorVoid {
    private val dumper = DecompileIrTreeVisitor(out)

    override fun visitElement(element: IrElement) {
        if (fileEntry.getLineNumber(element.startOffset) == lineNumber) {
            element.accept(dumper, "")
            return
        }

        element.acceptChildrenVoid(this)
    }
}

internal fun IrMemberAccessExpression.getValueParameterNamesForDebug(): List<String> {
    val expectedCount = valueArgumentsCount
    return if (this is IrDeclarationReference && symbol.isBound) {
        val owner = symbol.owner
        if (owner is IrFunction) {
            (0 until expectedCount).map {
                if (it < owner.valueParameters.size)
                    owner.valueParameters[it].name.asString()
                else
                    "${it + 1}"
            }
        } else {
            getPlaceholderParameterNames(expectedCount)
        }
    } else
        getPlaceholderParameterNames(expectedCount)
}

internal fun getPlaceholderParameterNames(expectedCount: Int) =
    (1..expectedCount).map { "$it" }
