/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.decompiler.decompile
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

private val defaultImportRegex = setOf(
    "kotlin\\.\\w+",
    "kotlin.annotation\\.\\w+",
    "kotlin.collections\\.\\w+",
    "kotlin.comparisons\\.\\w+",
    "kotlin.io\\.\\w+",
    "kotlin.ranges\\.\\w+",
    "kotlin.sequences\\.\\w+",
    "kotlin.text\\.\\w+"
).map { it.toRegex() }

class ImportResolveVisitor(val importDirectivesSet: MutableSet<String> = mutableSetOf()) : IrElementVisitor<Unit, List<String>> {
    private val declaredNamesSet = mutableSetOf<String>()
    private val calledNamesSet = mutableSetOf<String>()

    companion object {
        private fun MutableSet<String>.add(nullable: String?) {
            if (nullable != null) add(nullable)
        }

        private fun List<String?>.joinedWithDots() = filterNotNull().joinToString(".")

//        fun IrType.asImportStr() =
//            (this as? IrSimpleType)?.abbreviation?.typeAlias?.owner?.fqNameWhenAvailable?.asString()
//                ?: getClass()?.asImportStr()
//
//        fun IrClass.asImportStr() = fqNameWhenAvailable?.asString()?.takeIf { !isLocalClass() }
    }

    override fun visitFile(declaration: IrFile, data: List<String>) {
        with(declaration) {
            val scopeList = listOfNotNull(fqName.asString())
            declarations
                .forEach { it.accept(this@ImportResolveVisitor, scopeList) }

        }
        importDirectivesSet.addAll(calledNamesSet)
        importDirectivesSet.removeAll(declaredNamesSet)
        defaultImportRegex.forEach { regex ->
            importDirectivesSet.removeIf { it.matches(regex) }
        }
    }

    /**
     * Class definition processing includes adding a name to the collection of declarations
     * and sequential processing of supertypes and all members of the class.
     */
    override fun visitClass(declaration: IrClass, data: List<String>) {
        with(declaration) {
            val scopeList = data + declaration.name()
            declaredNamesSet.add(scopeList.joinedWithDots())
            // Для енамов в суперах лежит Enum<MyType>, который почему-то не isEnum
            //TODO обработка superTypes через коробочку
//            superTypes.filterNot { it.isAny() || it.toKotlinType().toString().startsWith("Enum") }
//                .forEach { calledNamesSet.add(it.asImportStr()) }
            declarations.filterNot {
                it.origin in setOf(
                    IrDeclarationOrigin.FAKE_OVERRIDE,
                    IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER,
                    IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER
                )
            }
                .forEach { it.accept(this@ImportResolveVisitor, scopeList) }
        }
    }

    override fun visitProperty(declaration: IrProperty, data: List<String>) {
        with(declaration) {
            val scopeList = data + name()
            if (parent is IrFile || parentAsClass.isCompanion || parentAsClass.isObject) {
                declaredNamesSet.add(scopeList.joinedWithDots())
            }
            backingField?.initializer?.accept(this@ImportResolveVisitor, scopeList)
            getter?.accept(this@ImportResolveVisitor, scopeList)
            setter?.accept(this@ImportResolveVisitor, scopeList)
        }
    }

    override fun visitConstructor(declaration: IrConstructor, data: List<String>) {
        with(declaration) {
            typeParameters.forEach { it.accept(this@ImportResolveVisitor, data) }
            valueParameters.forEach { it.accept(this@ImportResolveVisitor, data) }
            body?.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: List<String>) {
        //TODO Здесь может быть больше всего проблем (extension и dispatcher receivers, superQualifier)

        with(expression) {
            dispatchReceiver?.accept(this@ImportResolveVisitor, data)
            extensionReceiver?.accept(this@ImportResolveVisitor, data)
            when (origin) {
                IrStatementOrigin.GET_PROPERTY -> {
                    val fullName = symbol.owner.name()
                    val regex = """<get-(.+)>""".toRegex()
                    val matchResult = regex.find(fullName)
                    val propName = matchResult?.groups?.get(1)?.value
                    //TODO заменить на вставку в коробку
                    // calledNamesSet.add((data + propName).joinedWithDots())
                }
                else -> {
                    //TODO заменить на вставку в коробку expression.type и скоуп
                    calledNamesSet.add(data.joinedWithDots())
                }
            }
            //TODO заменить на вставку в коробку
//            (0 until typeArgumentsCount).forEach { calledNamesSet.add(getTypeArgument(it)?.asImportStr()) }
            //TODO Может ли быть конфликт при обработке аргументов не с местом вызова, а с вызывающим owner'ом?
            (0 until valueArgumentsCount).forEach { getValueArgument(it)?.accept(this@ImportResolveVisitor, data) }
        }
    }


    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: List<String>) {
        //TODO заменить на вставку в коробку IrType parent'а со скоупом
//        calledNamesSet.add(expression.symbol.owner.parentAsClass.asImportStr())
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: List<String>) {
        with(declaration) {
            val scopeList = data + name()
            declaredNamesSet.add(scopeList.joinedWithDots())
            //TODO Может ли быть конфликт при обработке аргументов не с местом вызова, а с вызывающим owner'ом?
            initializerExpression?.accept(this@ImportResolveVisitor, scopeList)
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: List<String>) {
        with(expression) {
            //TODO сделать вставку в коробку IrType symbol.owner.returnType'а со скоупом
            //
            //TODO Может ли быть конфликт при обработке аргументов не с местом вызова, а с вызывающим owner'ом?
            (0 until valueArgumentsCount).forEach { getValueArgument(it)?.accept(this@ImportResolveVisitor, data) }
        }
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: List<String>) {
        val ownerEnumEntry = expression.symbol.owner
        //TODO проверить, что можно получить из expression.type и как это сочетается со скоупом и помогает в резолве конфликтов
//            calledNamesSet.add(ownerEnumEntry.fqNameWhenAvailable?.asString() ?: name())
        with(ownerEnumEntry) {
            initializerExpression?.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitBody(body: IrBody, data: List<String>) {
        body.statements.forEach { it.accept(this, data) }
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: List<String>) {
        with(declaration) {
            //TODO заменить на вставку в коробку declaration.type'а со скоупом
//        calledNamesSet.add(declaration.type.asImportStr())
            defaultValue?.accept(this@ImportResolveVisitor, data)
        }

    }

    override fun visitFunction(declaration: IrFunction, data: List<String>) {
        with(declaration) {
            val scopeList = data + declaration.name()
            declaredNamesSet.add(data.joinedWithDots())
            typeParameters.forEach { it.accept(this@ImportResolveVisitor, scopeList) }
            valueParameters.forEach { it.accept(this@ImportResolveVisitor, scopeList) }
            body?.accept(this@ImportResolveVisitor, scopeList)
        }
    }

    override fun visitReturn(expression: IrReturn, data: List<String>) {
        expression.value.accept(this, data)
    }

    override fun visitWhen(expression: IrWhen, data: List<String>) {
        expression.branches.forEach { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: List<String>) {
        with(branch) {
            condition.accept(this@ImportResolveVisitor, data)
            result.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitVariable(declaration: IrVariable, data: List<String>) {
        //Здесь обрабатываем тип (потому что мог быть введен явно, а мы выводим явно всегда) и initializer
        with(declaration) {
            //TODO заменить на вставку в коробку declaration.type'а со скоупом
//            calledNamesSet.add(type.asImportStr())
            initializer?.accept(this@ImportResolveVisitor, data)
        }
    }

    //TODO присмотреться тщательнее к этому моменту
    override fun visitGetObjectValue(expression: IrGetObjectValue, data: List<String>) {
        with(expression) {
            val owner = symbol.owner
            if (!owner.isCompanion || owner.name() != "Companion") {
                //Здесь нужно учитывать локальные классы/методы?
                //TODO заменить на вставку в коробку owner.defaultType со скоупом
//                calledNamesSet.add(owner.asImportStr())
            } else {
                //Здесь проблема, если у companion object могут быть вложенные (но вроде не может)
                // или companion object может быть не только у класса
                //TODO заменить на вставку в коробку owner.parentAsClass.defaultType со скоупом
//                calledNamesSet.add(owner.parentAsClass.asImportStr())
            }
        }
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: List<String>) {
        with(declaration) {
            val scopeList = data + name()
            declaredNamesSet.add(scopeList.joinedWithDots())
            //TODO вставку в коробку expandedType со скоупом
            //
        }
    }

    override fun visitClassReference(expression: IrClassReference, data: List<String>) {
        //TODO вставка в коробку expression.classType со скоупом
//        calledNamesSet.add(expression.classType.asImportStr())
    }

    override fun visitBlockBody(body: IrBlockBody, data: List<String>) {
        body.acceptChildren(this, data)
    }

    override fun visitTry(aTry: IrTry, data: List<String>) {
        with(aTry) {
            tryResult.accept(this@ImportResolveVisitor, data)
            catches.forEach { it.accept(this@ImportResolveVisitor, data) }
            finallyExpression?.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitCatch(aCatch: IrCatch, data: List<String>) {
        with(aCatch) {
            catchParameter.accept(this@ImportResolveVisitor, data)
            result.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitThrow(expression: IrThrow, data: List<String>) {
        expression.value.accept(this, data)
    }

    override fun visitLoop(loop: IrLoop, data: List<String>) {
        with(loop) {
            condition.accept(this@ImportResolveVisitor, data)
            body?.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: List<String>) {
        with(expression) {
            argument.accept(this@ImportResolveVisitor, data)
            if (operator in listOf(
                    IrTypeOperator.CAST,
                    IrTypeOperator.SAFE_CAST,
                    IrTypeOperator.INSTANCEOF,
                    IrTypeOperator.NOT_INSTANCEOF
                )
            ) {
                // TODO вставка в коробку typeOperand со скоупом
                run {}
            }
        }
    }

    override fun visitCallableReference(expression: IrCallableReference, data: List<String>) {
        //TODO а когда я сюда вообще попадаю?
        run {}
    }

    override fun visitVararg(expression: IrVararg, data: List<String>) {
        expression.elements.forEach { it.accept(this, data) }
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: List<String>) {
        expression.statements.forEach { it.accept(this, data) }
    }

    override fun visitSetVariable(expression: IrSetVariable, data: List<String>) {
        expression.value.accept(this, data)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: List<String>) {
        //TODO добавить все declaration.superTypes в коробку вместе со скоупом

        run {}
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: List<String>) {
        expression.arguments.forEach { it.accept(this, data) }
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: List<String>) {
        spread.expression.accept(this, data)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: List<String>) {
        //TODO поставить breakpoint и посмотреть, когда сюда заходим
        run {}
    }

    override fun visitElement(element: IrElement, data: List<String>) {
        run { }
    }

}