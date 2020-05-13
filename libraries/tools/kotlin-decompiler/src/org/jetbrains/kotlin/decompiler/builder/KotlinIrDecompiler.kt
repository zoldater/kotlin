/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.builder

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.decompiler.printer.FileSourcesWriter
import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.decompiler.tree.declarations.classes.*
import org.jetbrains.kotlin.decompiler.tree.expressions.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.io.File

class KotlinIrDecompiler private constructor() {

    fun decompileIrModule(irModuleFragment: IrModuleFragment): DecompilerTreeModule {
        DecompilerTreeConstructionVisitor().apply {
            return irModuleFragment.buildElement(null)
        }
    }

    // private
    // не очень удачный нейминг, не очень понятно, при чём здесь Extension
    internal enum class ExtensionKind {
        FIELD_INIT,
        CUSTOM_GETTER,
        CUSTOM_SETTER,
        ENUM_ENTRY_INIT,
        ANNOTATION_CALL,
        DEFAULT_VALUE_ARGUMENT,
        DATA_CLASS_MEMBER,
        LAMBDA_CONTENT
    }

    // private
    internal class DecompilerTreeConstructionVisitor : IrElementVisitor<DecompilerTreeElement, ExtensionKind?> {
        private val elementsCacheMap = mutableMapOf<IrElement, DecompilerTreeElement>()
        private val typesCacheMap = mutableMapOf<IrType, DecompilerTreeType>()

        override fun visitElement(element: IrElement, data: ExtensionKind?): DecompilerTreeElement {
            /*
             * в таких местах лучше кидать IllegalStateException
             *
             * семантика следующая:
             * - TODO -- ещё не готово, но потом будет сделано
             * - IllegalStateException -- программа написана так, что сюда мы никогда не придём
             */
            TODO("Element $element was not properly built")
        }

        override fun visitModuleFragment(declaration: IrModuleFragment, data: ExtensionKind?): DecompilerTreeModule {
            val decompilerIrFiles =
                declaration.files.map { it.accept(this, data) as DecompilerTreeFile }
            return DecompilerTreeModule(declaration, decompilerIrFiles)
        }

        override fun visitPackageFragment(declaration: IrPackageFragment, data: ExtensionKind?): DecompilerTreeElement {
            TODO("Package $declaration was not properly built")
        }

        override fun visitFile(declaration: IrFile, data: ExtensionKind?): DecompilerTreeFile = with(declaration) {
            DecompilerTreeFile(this, decompileDeclarations(data), decompileAnnotations())
        }

        override fun visitScript(declaration: IrScript, data: ExtensionKind?): DecompilerTreeElement {
            TODO("Script $declaration was not properly built")
        }

        override fun visitDeclaration(declaration: IrDeclaration, data: ExtensionKind?): DecompilerTreeDeclaration {
            TODO("Declaration $declaration was not properly built")
        }

        override fun visitClass(declaration: IrClass, data: ExtensionKind?): AbstractDecompilerTreeClass = with(declaration) {
            when {
                kind == ClassKind.INTERFACE -> DecompilerTreeInterface(
                    this,
                    /*
                     * этот список аргументов повторяется 6 раз. стоит вынести в локальные переменные
                     * подобное замечание применимо и к функциям ниже
                     */
                    decompileDeclarations(data),
                    decompileAnnotations(),
                    buildTypeParameters(data),
                    buildThisReceiver(data),
                    buildSuperTypes()
                )
                kind == ClassKind.ENUM_CLASS -> DecompilerTreeEnumClass(
                    this,
                    decompileDeclarations(data),
                    decompileAnnotations(),
                    buildTypeParameters(data),
                    buildThisReceiver(data),
                    buildSuperTypes()
                )
                kind == ClassKind.ANNOTATION_CLASS -> DecompilerTreeAnnotationClass(
                    this,
                    decompileDeclarations(data),
                    decompileAnnotations(),
                    buildTypeParameters(data),
                    buildThisReceiver(data),
                    buildSuperTypes()
                )
                //TODO is it enough for `object SomeObj` val x = object : Any {...}
                kind == ClassKind.OBJECT -> DecompilerTreeObject(
                    this,
                    decompileDeclarations(data),
                    decompileAnnotations(),
                    buildTypeParameters(data),
                    buildThisReceiver(data),
                    buildSuperTypes()
                )
                isData -> DecompilerTreeDataClass(
                    this,
                    decompileDeclarations(ExtensionKind.DATA_CLASS_MEMBER),
                    decompileAnnotations(),
                    buildTypeParameters(data),
                    buildThisReceiver(data),
                    buildSuperTypes()
                )
                else -> DecompilerTreeClass(
                    this,
                    decompileDeclarations(data),
                    decompileAnnotations(),
                    buildTypeParameters(data),
                    buildThisReceiver(data),
                    buildSuperTypes()
                )
            }
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: ExtensionKind?): AbstractDecompilerTreeSimpleFunction =
            with(declaration) {
                when (data) {
                    ExtensionKind.LAMBDA_CONTENT -> DecompilerTreeLambdaFunction(
                        this, returnType.buildType(), dispatchReceiverParameter?.buildValueParameter(data),
                        extensionReceiverParameter?.buildValueParameter(data),
                        valueParameters.buildValueParameters(data),
                        body?.buildElement(data)
                    )
                    ExtensionKind.CUSTOM_GETTER -> DecompilerTreeCustomGetter(
                        this, decompileAnnotations(), returnType.buildType(),
                        dispatchReceiverParameter?.buildValueParameter(data),
                        extensionReceiverParameter?.buildValueParameter(data),
                        valueParameters.buildValueParameters(data),
                        body?.buildElement(data), //TODO is it necessary to implicitly declare <IrBody, DecompilerTreeBody>?
                        buildTypeParameters(data)
                    )
                    ExtensionKind.CUSTOM_SETTER -> DecompilerTreeCustomSetter(
                        this, decompileAnnotations(), returnType.buildType(),
                        dispatchReceiverParameter?.buildValueParameter(data),
                        extensionReceiverParameter?.buildValueParameter(data),
                        valueParameters.buildValueParameters(data),
                        body?.buildElement(data), //TODO is it necessary to implicitly declare <IrBody, DecompilerTreeBody>?
                        buildTypeParameters(data)
                    )
                    else -> DecompilerTreeSimpleFunction(
                        this, decompileAnnotations(), returnType.buildType(),
                        dispatchReceiverParameter?.buildValueParameter(data),
                        extensionReceiverParameter?.buildValueParameter(data),
                        valueParameters.buildValueParameters(data),
                        body?.buildElement(data), //TODO is it necessary to implicitly declare <IrBody, DecompilerTreeBody>?
                        buildTypeParameters(data)
                    )
                }
            }

        override fun visitConstructor(declaration: IrConstructor, data: ExtensionKind?): AbstractDecompilerTreeConstructor {
            val replacementKind = if (data == ExtensionKind.DATA_CLASS_MEMBER) data else ExtensionKind.DEFAULT_VALUE_ARGUMENT
            return with(declaration) {
                if (isPrimary && data == ExtensionKind.DATA_CLASS_MEMBER) DecompilerTreeDataClassPrimaryConstructor(
                    this,
                    decompileAnnotations(),
                    returnType.buildType(),
                    dispatchReceiverParameter?.buildValueParameter(data),
                    extensionReceiverParameter?.buildValueParameter(data),
                    valueParameters.buildValueParameters(replacementKind),
                    body?.buildElement(data),
                    buildTypeParameters(data)
                )
                else if (isPrimary) DecompilerTreePrimaryConstructor(
                    this,
                    decompileAnnotations(),
                    returnType.buildType(),
                    dispatchReceiverParameter?.buildValueParameter(data),
                    extensionReceiverParameter?.buildValueParameter(data),
                    valueParameters.buildValueParameters(replacementKind),
                    body?.buildElement(data),
                    buildTypeParameters(data)
                ) else DecompilerTreeSecondaryConstructor(
                    this,
                    decompileAnnotations(),
                    returnType.buildType(),
                    dispatchReceiverParameter?.buildValueParameter(data),
                    extensionReceiverParameter?.buildValueParameter(data),
                    valueParameters.buildValueParameters(replacementKind),
                    body?.buildElement(data),
                    buildTypeParameters(data)
                )
            }
        }


        override fun visitProperty(declaration: IrProperty, data: ExtensionKind?): DecompilerTreeProperty = with(declaration) {
            DecompilerTreeProperty(
                this,
                decompileAnnotations(),
                backingField?.buildElement(data),
                getter?.buildElement(ExtensionKind.CUSTOM_GETTER),
                setter?.buildElement(ExtensionKind.CUSTOM_SETTER)
            )
        }

        override fun visitField(declaration: IrField, data: ExtensionKind?): DecompilerTreeField = with(declaration) {
            DecompilerTreeField(this, decompileAnnotations(), initializer?.buildElement(ExtensionKind.FIELD_INIT), type.buildType())
        }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: ExtensionKind?): DecompilerTreeElement {
            TODO("Local delegated property $declaration was not built")
        }

        override fun visitVariable(declaration: IrVariable, data: ExtensionKind?): AbstractDecompilerTreeVariable = with(declaration) {
            DecompilerTreeVariable(this, decompileAnnotations(), initializer?.buildExpression(data), type.buildType())
        }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: ExtensionKind?): DecompilerTreeEnumEntry = with(declaration) {
            DecompilerTreeEnumEntry(
                this,
                decompileAnnotations(),
                initializerExpression?.buildElement(ExtensionKind.ENUM_ENTRY_INIT)
            )
        }

        override fun visitAnonymousInitializer(
            declaration: IrAnonymousInitializer,
            data: ExtensionKind?
        ): DecompilerTreeAnonymousInitializer = with(declaration) {
            DecompilerTreeAnonymousInitializer(this, body.buildElement(data))
        }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: ExtensionKind?): DecompilerTreeTypeParameter =
            with(declaration) {
                DecompilerTreeTypeParameter(this, decompileAnnotations())
            }

        override fun visitValueParameter(declaration: IrValueParameter, data: ExtensionKind?): AbstractDecompilerTreeValueParameter =
            with(declaration) {
                when (data) {
                    ExtensionKind.DATA_CLASS_MEMBER -> DecompilerTreePropertyValueParameter(
                        this,
                        decompileAnnotations(),
                        //TODO calculate annotation target
                        defaultValue?.buildElement(data),
                        type.buildType(),
                        varargElementType?.buildType()
                    )
                    else -> DecompilerTreeValueParameter(
                        this,
                        decompileAnnotations(),
                        //TODO calculate annotation target
                        defaultValue?.buildElement(data),
                        type.buildType(),
                        varargElementType?.buildType()
                    )
                }
            }

        override fun visitTypeAlias(declaration: IrTypeAlias, data: ExtensionKind?): DecompilerTreeTypeAlias = with(declaration) {
            DecompilerTreeTypeAlias(
                this, decompileAnnotations(),
                aliasedType = expandedType.buildType(),
                typeParameters = buildTypeParameters(data)
            )
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: ExtensionKind?): AbstractDecompilerTreeExpressionBody = with(body) {
            when (data) {
                ExtensionKind.ENUM_ENTRY_INIT -> DecompilerTreeEnumEntryInitializer(this, expression.buildElement(data))
                ExtensionKind.FIELD_INIT -> DecompilerTreeFieldInitializer(this, expression.buildElement(data))
                ExtensionKind.DEFAULT_VALUE_ARGUMENT, ExtensionKind.DATA_CLASS_MEMBER -> DecompilerTreeDefaultValueParameterInitializer(
                    this,
                    expression.buildElement(data)
                )
                else -> DecompilerTreeExpressionBody(this, expression.buildExpression(data))
            }
        }

        override fun visitBlockBody(body: IrBlockBody, data: ExtensionKind?): AbstractDecompilerTreeBlockBody = with(body) {
            when {
                data == ExtensionKind.CUSTOM_GETTER -> DecompilerTreeGetterBody(this, statements.buildElements(data))
                data == ExtensionKind.CUSTOM_SETTER -> DecompilerTreeSetterBody(this, statements.buildElements(data))
                else -> DecompilerTreeBlockBody(this, statements.buildElements(data))
            }

        }

        override fun visitSyntheticBody(body: IrSyntheticBody, data: ExtensionKind?): DecompilerTreeSyntheticBody = with(body) {
            DecompilerTreeSyntheticBody(this)
        }

        override fun visitExpression(expression: IrExpression, data: ExtensionKind?): DecompilerTreeExpression {
            TODO("Expression $expression was not properly built")
        }

        override fun <T> visitConst(expression: IrConst<T>, data: ExtensionKind?): DecompilerTreeConst =
            DecompilerTreeConst(expression, expression.type.buildType())

        override fun visitVararg(expression: IrVararg, data: ExtensionKind?): DecompilerTreeVararg = with(expression) {
            return DecompilerTreeVararg(this, elements.buildElements(data), type.buildType())
        }

        override fun visitSpreadElement(spread: IrSpreadElement, data: ExtensionKind?): DecompilerTreeSpread = with(spread) {
            val decompilerIrExpression = expression.buildExpression(data)
            DecompilerTreeSpread(this, decompilerIrExpression)
        }

        override fun visitContainerExpression(
            expression: IrContainerExpression,
            data: ExtensionKind?
        ): AbstractDecompilerTreeContainerExpression =
            with(expression) {
                when (origin) {
                    IrStatementOrigin.WHEN -> DecompilerTreeWhenContainer(this, statements.buildElements(data), type.buildType())
                    IrStatementOrigin.FOR_LOOP_INNER_WHILE -> DecompilerTreeForLoopInnerContainer(
                        this,
                        statements.buildElements(data),
                        type.buildType()
                    )
                    IrStatementOrigin.FOR_LOOP -> DecompilerTreeForLoopOuterContainer(
                        this,
                        statements.buildElements(data),
                        type.buildType()
                    )
                    IrStatementOrigin.ELVIS -> DecompilerTreeElvisOperatorCallContainer(
                        type.buildType(),
                        DecompilerTreeElvisOperatorCallExpression(
                            type.buildType(),
                            statements[0].buildElement(null),
                            statements[1].buildElement(null)
                        )
                    )
                    IrStatementOrigin.SAFE_CALL -> DecompilerTreeSafeCallOperatorContainer(
                        type.buildType(),
                        DecompilerTreeSafeCallOperatorExpression(
                            type.buildType(),
                            statements[0].buildElement(null),
                            statements[1].buildElement(null)
                        )
                    )
                    else -> DecompilerTreeContainerExpression(this, statements.buildElements(data), type.buildType())
                }
            }

        override fun visitStringConcatenation(expression: IrStringConcatenation, data: ExtensionKind?): DecompilerTreeStringConcatenation {
            val arguments = expression.arguments.buildExpressions(data)
            return DecompilerTreeStringConcatenation(expression, arguments, expression.type.buildType())
        }

        override fun visitGetObjectValue(expression: IrGetObjectValue, data: ExtensionKind?): DecompilerTreeGetObjectValue =
            with(expression) {
                val parent = symbol.owner.buildElement<IrClass, DecompilerTreeObject>(data)
                return DecompilerTreeGetObjectValue(this, parent, type.buildType())
            }

        override fun visitGetEnumValue(expression: IrGetEnumValue, data: ExtensionKind?): DecompilerTreeGetEnumValue = with(expression) {
            val parent = symbol.owner.buildElement<IrEnumEntry, DecompilerTreeEnumEntry>(data)
            return DecompilerTreeGetEnumValue(this, parent, type.buildType())
        }

        override fun visitGetValue(expression: IrGetValue, data: ExtensionKind?): DecompilerTreeGetValue =
            DecompilerTreeGetValue(expression, expression.type.buildType())

        override fun visitSetVariable(expression: IrSetVariable, data: ExtensionKind?): DecompilerTreeSetVariable = with(expression) {
            DecompilerTreeSetVariable(this, value.buildExpression(data), type.buildType())
        }

        override fun visitGetField(expression: IrGetField, data: ExtensionKind?): AbstractDecompilerTreeGetField = with(expression) {
            when (data) {
                ExtensionKind.CUSTOM_GETTER -> DecompilerTreeGetFieldFromGetterSetter(
                    this,
                    receiver?.buildExpression(data),
                    type.buildType()
                )
                else -> DecompilerTreeGetFieldCommon(this, receiver?.buildExpression(data), type.buildType())
            }
        }

        override fun visitSetField(expression: IrSetField, data: ExtensionKind?): AbstractDecompilerTreeSetField = with(expression) {
            when (data) {
                ExtensionKind.CUSTOM_SETTER -> DecompilerTreeSetFieldFromGetterSetter(
                    this,
                    receiver?.buildExpression(data),
                    value.buildExpression(data),
                    type.buildType()
                )
                else -> DecompilerTreeSetFieldCommon(this, receiver?.buildExpression(data), value.buildExpression(data), type.buildType())
            }
        }

        override fun visitCall(expression: IrCall, data: ExtensionKind?): AbstractDecompilerTreeCall = with(expression) {
            buildCall(
                buildDispatchReceiver(data),
                buildExtensionReceiver(data),
                buildValueArguments(data),
                type.buildType(),
                buildTypeArguments()
            )
        }

        override fun visitConstructorCall(expression: IrConstructorCall, data: ExtensionKind?): AbstractDecompilerTreeConstructorCall =
            with(expression) {
                when (data) {
                    ExtensionKind.ANNOTATION_CALL -> DecompilerTreeAnnotationConstructorCall(
                        this,
                        buildDispatchReceiver(data),
                        buildExtensionReceiver(data),
                        buildValueArguments(data),
                        type.buildType(),
                        buildTypeArguments()
                    )
                    else -> DecompilerTreeCommonConstructorCall(
                        this,
                        buildDispatchReceiver(data),
                        buildExtensionReceiver(data),
                        buildValueArguments(data),
                        type.buildType(),
                        buildTypeArguments()
                    )

                }
            }

        override fun visitDelegatingConstructorCall(
            expression: IrDelegatingConstructorCall,
            data: ExtensionKind?
        ): DecompilerTreeDelegatingConstructorCall = with(expression) {
            DecompilerTreeDelegatingConstructorCall(
                this,
                buildDispatchReceiver(data),
                buildExtensionReceiver(data),
                buildValueArguments(data),
                type.buildType(),
                buildTypeArguments(),
                symbol.owner.returnType.buildType()
            )
        }

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: ExtensionKind?): DecompilerTreeEnumConstructorCall =
            with(expression) {
                DecompilerTreeEnumConstructorCall(
                    this,
                    buildDispatchReceiver(data),
                    buildExtensionReceiver(data),
                    buildValueArguments(data),
                    type.buildType()
                )
            }

        override fun visitGetClass(expression: IrGetClass, data: ExtensionKind?): DecompilerTreeGetClass = with(expression) {
            DecompilerTreeGetClass(this, argument.buildExpression(data), type.buildType())
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: ExtensionKind?): DecompilerTreeFunctionReference =
            with(expression) {
                DecompilerTreeFunctionReference(
                    this,
                    buildDispatchReceiver(data),
                    buildExtensionReceiver(data),
                    buildValueArguments(data),
                    type.buildType(),
                    buildTypeArguments()
                )
            }

        override fun visitPropertyReference(expression: IrPropertyReference, data: ExtensionKind?): DecompilerTreePropertyReference =
            with(expression) {
                DecompilerTreePropertyReference(
                    this,
                    buildDispatchReceiver(data),
                    buildExtensionReceiver(data),
                    buildValueArguments(data),
                    type.buildType(),
                    buildTypeArguments()
                )
            }

        override fun visitLocalDelegatedPropertyReference(
            expression: IrLocalDelegatedPropertyReference,
            data: ExtensionKind?
        ): DecompilerTreeLocalDelegatedPropertyReference =
            with(expression) {
                DecompilerTreeLocalDelegatedPropertyReference(
                    this,
                    buildDispatchReceiver(data),
                    buildExtensionReceiver(data),
                    buildValueArguments(data),
                    type.buildType(),
                    buildTypeArguments()
                )
            }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: ExtensionKind?): DecompilerTreeFunctionExpression =
            with(expression) {
                DecompilerTreeFunctionExpression(this, function.buildElement(ExtensionKind.LAMBDA_CONTENT), type.buildType())
            }


        override fun visitClassReference(expression: IrClassReference, data: ExtensionKind?): DecompilerTreeClassReference =
            DecompilerTreeClassReference(expression, expression.type.buildType(), expression.classType.buildType())

        override fun visitInstanceInitializerCall(
            expression: IrInstanceInitializerCall,
            data: ExtensionKind?
        ): DecompilerTreeInstanceInitializerCall = DecompilerTreeInstanceInitializerCall(expression, expression.type.buildType())


        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: ExtensionKind?): DecompilerTreeTypeOperatorCall =
            with(expression) {
                DecompilerTreeTypeOperatorCall(this, argument.buildExpression(data), type.buildType(), typeOperand.buildType())
            }

        override fun visitWhen(expression: IrWhen, data: ExtensionKind?): AbstractDecompilerTreeWhen {
            val branches =
                expression.branches.buildElements<IrBranch, AbstractDecompilerTreeBranch>(data)
            return when (expression) {
                is IrIfThenElseImpl -> DecompilerTreeIfThenElse(expression, branches, expression.type.buildType())
                else -> DecompilerTreeWhen(expression, branches, expression.type.buildType())
            }
        }

        override fun visitBranch(branch: IrBranch, data: ExtensionKind?): AbstractDecompilerTreeBranch = with(branch) {
            DecompilerTreeBranch(branch, condition.buildExpression(data), result.buildExpression(data))
        }

        override fun visitElseBranch(branch: IrElseBranch, data: ExtensionKind?): DecompilerTreeElseBranch = with(branch) {
            DecompilerTreeElseBranch(branch, condition.buildExpression(data), result.buildExpression(data))
        }

        override fun visitWhileLoop(loop: IrWhileLoop, data: ExtensionKind?): DecompilerTreeWhileLoop = with(loop) {
            DecompilerTreeWhileLoop(this, condition.buildExpression(data), body?.buildExpression(data), type.buildType())
        }

        override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: ExtensionKind?): DecompilerTreeDoWhileLoop = with(loop) {
            DecompilerTreeDoWhileLoop(this, condition.buildExpression(data), body?.buildExpression(data), type.buildType())
        }

        override fun visitTry(aTry: IrTry, data: ExtensionKind?): DecompilerTreeTry = with(aTry) {
            DecompilerTreeTry(
                this,
                tryResult.buildExpression(data),
                catches.buildElements(data),
                finallyExpression?.buildExpression(data),
                type.buildType()
            )
        }

        override fun visitCatch(aCatch: IrCatch, data: ExtensionKind?): DecompilerTreeCatch {
            with(aCatch) {
                val dirCatchParameter = DecompilerTreeCatchParameterVariable(
                    catchParameter,
                    catchParameter.decompileAnnotations(),
                    catchParameter.type.buildType()
                )
                val dirResult = aCatch.result.buildExpression(data)
                return DecompilerTreeCatch(this, dirCatchParameter, dirResult)
            }
        }

        override fun visitBreak(jump: IrBreak, data: ExtensionKind?): DecompilerTreeBreak = DecompilerTreeBreak(jump, jump.type.buildType())

        override fun visitContinue(jump: IrContinue, data: ExtensionKind?): DecompilerTreeContinue =
            DecompilerTreeContinue(jump, jump.type.buildType())

        override fun visitReturn(expression: IrReturn, data: ExtensionKind?): AbstractDecompilerTreeReturn = with(expression) {
            when (data) {
                ExtensionKind.CUSTOM_GETTER -> DecompilerTreeGetterReturn(this, value.buildExpression(data), type.buildType())
                else -> DecompilerTreeReturn(this, value.buildExpression(data), type.buildType())
            }
        }

        override fun visitThrow(expression: IrThrow, data: ExtensionKind?): DecompilerTreeThrow {
            val throwable = expression.value.buildExpression(data)
            return DecompilerTreeThrow(expression, throwable, expression.type.buildType())
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T : IrType, R : DecompilerTreeType> T.buildType(): R =
            (typesCacheMap[this] ?: run {
                val existingClass = elementsCacheMap.values
                    .filterIsInstance(AbstractDecompilerTreeClass::class.java)
                    .find { it.element.defaultType == this }
                when {
                    toKotlinType().isFunctionTypeOrSubtype -> DecompilerTreeFunctionalType(this)
                    else -> DecompilerTreeSimpleType(this, existingClass)
                }.also {
                    typesCacheMap += this to it
                }
            }) as R


        @Suppress("UNCHECKED_CAST")
        internal fun <T : IrElement, R : DecompilerTreeElement> T.buildElement(kind: ExtensionKind?): R =
            (elementsCacheMap[this] ?: run {
                accept(this@DecompilerTreeConstructionVisitor, kind).also {
                    elementsCacheMap += this to it
                    (it as? AbstractDecompilerTreeClass)?.element?.defaultType?.also { clsType ->
                        if (typesCacheMap.containsKey(clsType)) typesCacheMap[clsType]!!.typeClassIfExists = it
                        else typesCacheMap += clsType to DecompilerTreeSimpleType(clsType, it)
                    }
                }
            }) as R

        private fun <T : IrElement, R : DecompilerTreeElement> Iterable<T>.buildElements(kind: ExtensionKind?): List<R> =
            map { it.buildElement(kind) }

        private fun IrDeclaration.buildDeclaration(kind: ExtensionKind?) =
            buildElement<IrDeclaration, DecompilerTreeDeclaration>(kind)

        private fun Iterable<IrDeclaration>.buildDeclarations(kind: ExtensionKind?) =
            map { it.buildDeclaration(kind) }

        private fun IrValueParameter.buildValueParameter(kind: ExtensionKind?) =
            buildElement<IrValueParameter, AbstractDecompilerTreeValueParameter>(kind)

        private fun Iterable<IrValueParameter>.buildValueParameters(kind: ExtensionKind?) =
            map { it.buildValueParameter(kind) }


        private fun IrExpression.buildExpression(kind: ExtensionKind?) =
            buildElement<IrExpression, DecompilerTreeExpression>(kind)

        private fun Iterable<IrExpression>.buildExpressions(kind: ExtensionKind?) =
            map { it.buildExpression(kind) }

        //TODO check inferred type correctness
        private fun IrAnnotationContainer.decompileAnnotations(): List<DecompilerTreeAnnotationConstructorCall> =
            annotations.buildElements(ExtensionKind.ANNOTATION_CALL)

        private fun IrDeclarationContainer.decompileDeclarations(kind: ExtensionKind?): List<DecompilerTreeDeclaration> =
            declarations.buildDeclarations(kind)

        private fun IrMemberAccessExpression.buildValueArguments(kind: ExtensionKind?) =
            (0 until valueArgumentsCount).mapNotNull { getValueArgument(it) }.buildExpressions(kind)

        private fun IrMemberAccessExpression.buildTypeArguments() =
            (0 until typeArgumentsCount).mapNotNull { getTypeArgument(it)?.buildType() }

        private fun IrMemberAccessExpression.buildDispatchReceiver(kind: ExtensionKind?): DecompilerTreeExpression? =
            dispatchReceiver?.buildExpression(kind)

        private fun IrMemberAccessExpression.buildExtensionReceiver(kind: ExtensionKind?): DecompilerTreeExpression? =
            extensionReceiver?.buildExpression(kind)

        private fun IrTypeParametersContainer.buildTypeParameters(kind: ExtensionKind?): List<DecompilerTreeTypeParameter> =
            typeParameters.buildElements(kind)

        private fun IrClass.buildThisReceiver(kind: ExtensionKind?): AbstractDecompilerTreeValueParameter? =
            thisReceiver?.buildValueParameter(kind)

        private fun IrClass.buildSuperTypes(): List<DecompilerTreeType> = superTypes.map { it.buildType() }

    }

    companion object {
        private val INSTANCE: KotlinIrDecompiler = KotlinIrDecompiler()

        fun decompileIrToString(irModuleFragment: IrModuleFragment): String {
            val decompilerTree = INSTANCE.decompileIrModule(irModuleFragment)
            return FileSourcesWriter(decompilerTree.decompilerIrFiles).filesToContentMap
                .map { (k, v) -> "// FILE: ${k.element.path}\n$v" }
                .joinToString("\n")

        }

        fun decompileToFileHierarchy(irModuleFragment: IrModuleFragment, path: String): File {
            val decompilerTreeModule = INSTANCE.decompileIrModule(irModuleFragment)
            FileSourcesWriter(decompilerTreeModule.decompilerIrFiles).writeFileSources(path).also {
                check(it.exists() && it.isDirectory && it.walkTopDown().count() > 0) { "Decompiled module produced empty hierarchy" }
                return it
            }
        }


    }
}