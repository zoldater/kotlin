/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.decompiler.DecompileIrTreeVisitor.Companion.obtainDescriptionForScope
import org.jetbrains.kotlin.decompiler.decompile
import org.jetbrains.kotlin.decompiler.util.magicbox.IMagicBox
import org.jetbrains.kotlin.decompiler.util.magicbox.MagicBoxImpl
import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor


class ImportResolveVisitor(val magicBox: IMagicBox = MagicBoxImpl()) : IrElementVisitor<Unit, List<String>>, IMagicBox by magicBox {

    override fun visitDeclarationReference(expression: IrDeclarationReference, data: List<String>) {
        TODO("Implement visitor for $expression")
    }

    override fun visitGetField(expression: IrGetField, data: List<String>) {
        expression.receiver?.accept(this, data)
    }

    override fun visitSetField(expression: IrSetField, data: List<String>) {
        expression.value.accept(this, data)
        expression.receiver?.accept(this, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: List<String>) {
        //Вроде ничего не нужно делать
    }

    override fun visitFile(declaration: IrFile, data: List<String>) {
        declaration.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: List<String>) {
        with(declaration) {
            val scopeList = data + declaration.name()
            putDeclarationWithName(scopeList, this)
            // Для енамов в суперах лежит Enum<MyType>, который почему-то не isEnum
            //TODO обработка superTypes через коробочку
            superTypes.filterNot { it.isAny() || it.toKotlinType().toString().startsWith("Enum") }
                .forEach { putExplicitTypeWithScope(scopeList, (it as IrSimpleType)) }
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
            val scopeList = data
            if (parent is IrFile || parentAsClass.isCompanion || parentAsClass.isObject) {
                putDeclarationWithName(scopeList, this)
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
            putCalledDeclarationReferenceWithScope(data, this)
            (0 until typeArgumentsCount).forEach { putExplicitTypeWithScope(data, (getTypeArgument(it) as IrSimpleType)) }
            //TODO Может ли быть конфликт при обработке аргументов не с местом вызова, а с вызывающим owner'ом?
            (0 until valueArgumentsCount).forEach { getValueArgument(it)?.accept(this@ImportResolveVisitor, data) }
        }
    }


    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: List<String>) {
        putCalledDeclarationReferenceWithScope(data, expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: List<String>) {
        with(expression) {
            dispatchReceiver?.accept(this@ImportResolveVisitor, data)
            putCalledDeclarationReferenceWithScope(data, this)
            (0 until typeArgumentsCount).forEach {
                putExplicitTypeWithScope(data, (getTypeArgument(it) as IrSimpleType))
            }
            //TODO добавить проверку наличия defaultValue и именнованных вызовов
            (0 until expression.valueArgumentsCount)
                .forEach { getValueArgument(it)?.accept(this@ImportResolveVisitor, data) }
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: List<String>) {
        with(declaration) {
            val scopeList = data
            putDeclarationWithName(scopeList, declaration)
            //TODO Может ли быть конфликт при обработке аргументов не с местом вызова, а с вызывающим owner'ом?
            initializerExpression?.accept(this@ImportResolveVisitor, scopeList)
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: List<String>) {
        with(expression) {
            putExplicitTypeWithScope(data, symbol.owner.returnType as IrSimpleType)
            (0 until typeArgumentsCount).forEach { putExplicitTypeWithScope(data, (getTypeArgument(it) as IrSimpleType)) }
            //TODO Может ли быть конфликт при обработке аргументов не с местом вызова, а с вызывающим owner'ом?
            (0 until valueArgumentsCount).forEach { getValueArgument(it)?.accept(this@ImportResolveVisitor, data) }
        }
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: List<String>) {
        putCalledDeclarationReferenceWithScope(data, expression)
    }

    override fun visitBody(body: IrBody, data: List<String>) {
        body.statements.forEach { it.accept(this, data) }
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: List<String>) {
        with(declaration) {
            putExplicitTypeWithScope(data, type as IrSimpleType)
            defaultValue?.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitFunction(declaration: IrFunction, data: List<String>) {
        with(declaration) {
            putDeclarationWithName(data, declaration)
            putExplicitTypeWithScope(data, returnType as IrSimpleType)
            //TODO тут нужен data или scopeList? Когда это может сломаться?
            typeParameters.forEach { it.accept(this@ImportResolveVisitor, data) }
            explicitParameters.forEach { it.accept(this@ImportResolveVisitor, data) }
            body?.accept(this@ImportResolveVisitor, data)
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
            putExplicitTypeWithScope(data, type as IrSimpleType)
            initializer?.accept(this@ImportResolveVisitor, data)
        }
    }

    //TODO присмотреться тщательнее к этому моменту
    override fun visitGetObjectValue(expression: IrGetObjectValue, data: List<String>) {
        putCalledDeclarationReferenceWithScope(data, expression)
    }


    override fun visitTypeAlias(declaration: IrTypeAlias, data: List<String>) {
        with(declaration) {
            val scopeList = data
            putDeclarationWithName(scopeList, declaration)
//            putExplicitTypeWithScope(scopeList, expandedType)
        }
    }

    override fun visitBlockBody(body: IrBlockBody, data: List<String>) {
        body.statements.forEach { it.accept(this, data) }
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
            when (operator) {
                CAST, SAFE_CAST, INSTANCEOF, NOT_INSTANCEOF -> putExplicitTypeWithScope(data, typeOperand as IrSimpleType)
                else -> {
                }
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

    override fun visitClassReference(expression: IrClassReference, data: List<String>) {
        TODO("Not implemented yet!")
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: List<String>) {
        TODO("Not implemented yet!")
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: List<String>) {
        TODO("Not implemented yet!")

    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: List<String>) {
        TODO("Not implemented yet!")
    }


}