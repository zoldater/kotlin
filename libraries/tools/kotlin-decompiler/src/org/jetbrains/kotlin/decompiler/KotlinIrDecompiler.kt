/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.decompiler.KotlinIrDecompiler.ExtraData.*
import org.jetbrains.kotlin.decompiler.printer.FileSourcesWriter
import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.decompiler.tree.declarations.classes.*
import org.jetbrains.kotlin.decompiler.tree.expressions.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.*
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.io.File
import java.util.*

class KotlinIrDecompiler private constructor() {

    fun decompileIrModule(irModuleFragment: IrModuleFragment): DecompilerTreeModule {
        DecompilerTreeConstructionVisitor().apply {
            return irModuleFragment.buildElement(null)
        }
    }

    internal enum class ExtraData {
        FIELD_INIT,
        ANONYMOUS_OBJECT,
        CUSTOM_GETTER,
        CUSTOM_SETTER,
        ENUM_ENTRY_INIT,
        ANNOTATION_CALL,
        PARAMETER_DEFAULT_VALUE,
        PROPERTY_PARAMETER,
        PRIMARY_CTOR_MEMBER,
        SECONDARY_CTOR_MEMBER,
        WHEN_SUBJECT_MEMBER,
        LAMBDA_CONTENT
    }

    private class DecompilerTreeConstructionVisitor : IrElementVisitor<DecompilerTreeElement, ExtraData?> {
        private val elementsCacheMap = WeakHashMap<IrElement, DecompilerTreeElement>()
        private val typesCacheMap = WeakHashMap<IrType, DecompilerTreeType>()

        override fun visitElement(element: IrElement, data: ExtraData?): DecompilerTreeElement {
            throw IllegalStateException("Element $element was not properly built")
        }

        override fun visitModuleFragment(declaration: IrModuleFragment, data: ExtraData?): DecompilerTreeModule {
            val decompilerIrFiles =
                declaration.files.map { it.accept(this, data) as DecompilerTreeFile }
            return DecompilerTreeModule(declaration, decompilerIrFiles)
        }

        override fun visitPackageFragment(declaration: IrPackageFragment, data: ExtraData?): DecompilerTreeElement {
            TODO("Package $declaration was not properly built")
        }

        override fun visitFile(declaration: IrFile, data: ExtraData?): DecompilerTreeFile = with(declaration) {
            DecompilerTreeFile(this, buildDeclarations(data), buildAnnotations())
        }

        override fun visitScript(declaration: IrScript, data: ExtraData?): DecompilerTreeElement {
            TODO("Script $declaration was not properly built")
        }

        override fun visitDeclaration(declaration: IrDeclaration, data: ExtraData?): DecompilerTreeDeclaration {
            TODO("Declaration $declaration was not properly built")
        }

        override fun visitClass(declaration: IrClass, data: ExtraData?): AbstractDecompilerTreeClass = with(declaration) {
            val annotations = buildAnnotations()
            val typeParameters = buildTypeParameters(data)
            val thisReceiver = buildThisReceiver(data)
            val superTypes = buildSuperTypes()
            val builtDeclarations = buildDeclarations(data)
            val configurator =
                DecompilerTreeClassConfiguration(this, builtDeclarations, annotations, superTypes, thisReceiver, typeParameters)
            return if (data == ANONYMOUS_OBJECT) DecompilerTreeAnonymousClass(configurator)
            else when (kind) {
                ClassKind.INTERFACE -> DecompilerTreeInterface(configurator)
                ClassKind.ENUM_CLASS -> DecompilerTreeEnumClass(configurator)
                ClassKind.ANNOTATION_CLASS -> DecompilerTreeAnnotationClass(configurator)
                ClassKind.OBJECT -> DecompilerTreeObject(configurator)
                else -> DecompilerTreeClass(configurator)
            }
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: ExtraData?): AbstractDecompilerTreeSimpleFunction =
            with(declaration) {
                val builtAnnotations = buildAnnotations()
                val builtReturnType = returnType.buildType<IrType, DecompilerTreeType>()
                val builtDispatchReceiver = dispatchReceiverParameter?.buildValueParameter(data)
                val builtExtensionReceiver = extensionReceiverParameter?.buildValueParameter(data)
                val builtValueParameters = valueParameters.buildValueParameters(data)
                val builtTypeParameters = buildTypeParameters(data)

                when (data) {
                    LAMBDA_CONTENT -> DecompilerTreeLambdaFunction(
                        this, builtReturnType, builtDispatchReceiver, builtExtensionReceiver, builtValueParameters, body?.buildElement(data)
                    )
                    CUSTOM_GETTER -> DecompilerTreeCustomGetter(
                        this, builtAnnotations, builtReturnType, builtDispatchReceiver, builtExtensionReceiver, builtValueParameters,
                        body?.buildElement(data), builtTypeParameters
                    )
                    CUSTOM_SETTER -> DecompilerTreeCustomSetter(
                        this, builtAnnotations, builtReturnType, builtDispatchReceiver, builtExtensionReceiver, builtValueParameters,
                        body?.buildElement(data), builtTypeParameters
                    )
                    else -> DecompilerTreeSimpleFunction(
                        this, builtAnnotations, builtReturnType, builtDispatchReceiver, builtExtensionReceiver, builtValueParameters,
                        body?.buildElement(data), builtTypeParameters
                    )
                }
            }

        override fun visitConstructor(declaration: IrConstructor, data: ExtraData?): AbstractDecompilerTreeConstructor {
            with(declaration) {
                val annotations = buildAnnotations()
                val returnType = returnType.buildType<IrType, DecompilerTreeType>()
                val dispatchReceiver = dispatchReceiverParameter?.buildValueParameter(data)
                val extensionReceiver = extensionReceiverParameter?.buildValueParameter(data)
                val typeParameters = buildTypeParameters(data)

                val parentClassProperties = parentAsClass.declarations.filterIsInstance<IrProperty>()
                val propertyNames = parentClassProperties.map { it.name() }
                val propertiesToGetValueNames = parentClassProperties.mapNotNull {
                    (it.backingField?.initializer?.expression as? IrGetValue)?.symbol?.owner?.name()
                        ?.let { getValueName -> getValueName to it }
                }.toMap()

                val valueParameters = valueParameters.map {
                    val valueParameterName = it.name()

                    if (valueParameterName in propertyNames && valueParameterName in propertiesToGetValueNames)
                        it.buildValueParameter(PROPERTY_PARAMETER)
                            .also { dvp ->
                                (dvp as DecompilerTreePropertyValueParameter).relatedProperty =
                                    propertiesToGetValueNames[valueParameterName]!!.buildElement(PROPERTY_PARAMETER)
                            }
                    else it.buildValueParameter(data)
                }


                return when {
                    isPrimary -> DecompilerTreePrimaryConstructor(
                        this,
                        annotations,
                        returnType,
                        dispatchReceiver,
                        extensionReceiver,
                        valueParameters,
                        body?.buildElement(PRIMARY_CTOR_MEMBER),
                        typeParameters
                    )
                    else -> DecompilerTreeSecondaryConstructor(
                        this,
                        annotations,
                        returnType,
                        dispatchReceiver,
                        extensionReceiver,
                        valueParameters,
                        body?.buildElement(SECONDARY_CTOR_MEMBER),
                        typeParameters
                    )
                }
            }
        }


        override fun visitProperty(declaration: IrProperty, data: ExtraData?): DecompilerTreeProperty = with(declaration) {
            DecompilerTreeProperty(
                this,
                buildAnnotations(),
                backingField?.buildElement(data),
                getter?.buildElement(CUSTOM_GETTER),
                setter?.buildElement(CUSTOM_SETTER),
                data == PROPERTY_PARAMETER
            )
        }

        override fun visitField(declaration: IrField, data: ExtraData?): DecompilerTreeField = with(declaration) {
            DecompilerTreeField(this, buildAnnotations(), initializer?.buildElement(FIELD_INIT), type.buildType())
        }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: ExtraData?): DecompilerTreeElement {
            TODO("Local delegated property $declaration was not built")
        }

        override fun visitVariable(declaration: IrVariable, data: ExtraData?): AbstractDecompilerTreeVariable = with(declaration) {
            DecompilerTreeVariable(this, buildAnnotations(), initializer?.buildExpression(data), type.buildType())
        }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: ExtraData?): DecompilerTreeEnumEntry = with(declaration) {
            DecompilerTreeEnumEntry(
                this,
                buildAnnotations(),
                initializerExpression?.buildElement(ENUM_ENTRY_INIT)
            )
        }

        override fun visitAnonymousInitializer(
            declaration: IrAnonymousInitializer,
            data: ExtraData?
        ): DecompilerTreeAnonymousInitializer = with(declaration) {
            DecompilerTreeAnonymousInitializer(this, body.buildElement(data))
        }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: ExtraData?): DecompilerTreeTypeParameter =
            with(declaration) {
                DecompilerTreeTypeParameter(this, buildAnnotations())
            }

        override fun visitValueParameter(declaration: IrValueParameter, data: ExtraData?): AbstractDecompilerTreeValueParameter =
            with(declaration) {
                when (data) {
                    PROPERTY_PARAMETER -> DecompilerTreePropertyValueParameter(
                        this,
                        buildAnnotations(),
                        //TODO calculate annotation target
                        defaultValue?.buildElement(PARAMETER_DEFAULT_VALUE),
                        type.buildType(),
                        varargElementType?.buildType()
                    )
                    else -> DecompilerTreeValueParameter(
                        this,
                        buildAnnotations(),
                        //TODO calculate annotation target
                        defaultValue?.buildElement(PARAMETER_DEFAULT_VALUE),
                        type.buildType(),
                        varargElementType?.buildType()
                    )
                }
            }

        override fun visitTypeAlias(declaration: IrTypeAlias, data: ExtraData?): DecompilerTreeTypeAlias = with(declaration) {
            DecompilerTreeTypeAlias(
                this, buildAnnotations(),
                aliasedType = expandedType.buildType(),
                typeParameters = buildTypeParameters(data)
            )
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: ExtraData?): AbstractDecompilerTreeExpressionBody = with(body) {
            when (data) {
                ENUM_ENTRY_INIT -> DecompilerTreeEnumEntryInitializer(this, expression.buildElement(data))
                FIELD_INIT -> DecompilerTreeFieldInitializer(this, expression.buildElement(data))
                PARAMETER_DEFAULT_VALUE -> DecompilerTreeDefaultValueParameterInitializer(
                    this,
                    expression.buildElement(data)
                )
                else -> DecompilerTreeExpressionBody(this, expression.buildExpression(data))
            }
        }

        override fun visitBlockBody(body: IrBlockBody, data: ExtraData?): AbstractDecompilerTreeBlockBody = with(body) {
            when (data) {
                CUSTOM_GETTER -> DecompilerTreeGetterBody(this, statements.buildElements(data))
                CUSTOM_SETTER -> DecompilerTreeSetterBody(this, statements.buildElements(data))
                else -> DecompilerTreeBlockBody(this, statements.buildElements(data))
            }

        }

        override fun visitSyntheticBody(body: IrSyntheticBody, data: ExtraData?): DecompilerTreeSyntheticBody = with(body) {
            DecompilerTreeSyntheticBody(this)
        }

        override fun visitExpression(expression: IrExpression, data: ExtraData?): DecompilerTreeExpression {
            TODO("Expression $expression was not properly built")
        }

        override fun <T> visitConst(expression: IrConst<T>, data: ExtraData?): DecompilerTreeConst =
            DecompilerTreeConst(expression, expression.type.buildType())

        override fun visitVararg(expression: IrVararg, data: ExtraData?): DecompilerTreeVararg = with(expression) {
            return DecompilerTreeVararg(this, elements.buildElements(data), type.buildType())
        }

        override fun visitSpreadElement(spread: IrSpreadElement, data: ExtraData?): DecompilerTreeSpread = with(spread) {
            val decompilerIrExpression = expression.buildExpression(data)
            DecompilerTreeSpread(this, decompilerIrExpression)
        }

        private fun IrContainerExpression.buildForLoopContainer(): DecompilerTreeForLoopContainer {
            check(IrDeclarationOrigin.FOR_LOOP_ITERATOR == (statements.getOrNull(0) as? IrVariable)?.origin) {
                "Unexpected For loop iterator synthetic variable!"
            }
            val initializer = (statements[0] as IrVariable).initializer
            check(FOR_LOOP_ITERATOR == (initializer as? IrMemberAccessExpression<*>)?.origin) {
                "Unexpected For loop iterator initializer!"
            }
            val sugaredInitializer = (initializer as IrMemberAccessExpression<*>).dispatchReceiver!!
            check(FOR_LOOP_INNER_WHILE == (statements.getOrNull(1) as? IrLoop)?.origin) {
                "Unexpected For loop structure!"
            }
            val innerWhileBody = (statements[1] as IrLoop).body
            check(FOR_LOOP_INNER_WHILE == (innerWhileBody as? IrContainerExpression)?.origin) {
                "Unexpected For loop inner while body structure!"
            }
            val innerWhileStatements = (innerWhileBody as IrContainerExpression).statements
            check(IrDeclarationOrigin.FOR_LOOP_VARIABLE == (innerWhileStatements.getOrNull(0) as? IrVariable)?.origin) {
                "Unexpected For loop original variable!"
            }
            val forLoopVariable = (innerWhileStatements[0] as IrVariable)
            val desugaredForVariable = DecompilerTreeForLoopVariable(
                forLoopVariable,
                forLoopVariable.buildAnnotations(),
                sugaredInitializer.buildExpression(),
                forLoopVariable.type.buildType()
            )
            val originalBlockStatements =
                (innerWhileStatements[1] as IrContainerExpression).statements.buildElements<IrStatement, DecompilerTreeStatement>()
            return DecompilerTreeForLoopContainer(this, desugaredForVariable, originalBlockStatements, this.type.buildType())

        }

        private fun IrContainerExpression.buildWhenContainer(): DecompilerTreeWhenContainer {
            val variable = statements.getOrNull(0) as? IrVariable
            val irWhen = statements[1] as IrWhen
            val builtBranches =
                irWhen.branches.map { it.buildElement<IrBranch, AbstractDecompilerTreeBranch>(WHEN_SUBJECT_MEMBER) }

            return when (variable?.origin) {
                IrDeclarationOrigin.DEFINED -> DecompilerTreeWhenContainer(
                    this, type.buildType(),
                    DecompilerTreeWhenWithSubjectVariable(irWhen, variable.buildElement(), builtBranches, type.buildType())
                )
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE -> DecompilerTreeWhenContainer(
                    this, type.buildType(),
                    DecompilerTreeWhenWithSubjectValue(
                        irWhen,
                        (variable.initializer as IrExpression).buildElement(),
                        builtBranches,
                        type.buildType()
                    )
                )
                else -> throw IllegalStateException("Bad When Container structure!")
            }
        }

        private fun IrContainerExpression.buildPostfixIncDecOperatorCallContainer(): DecompilerTreeIncDecOperatorsContainer {
            val tempVariable = statements[0]
            val tmpVarOrigin = (tempVariable as? IrVariable)?.origin
            check(IrDeclarationOrigin.IR_TEMPORARY_VARIABLE == tmpVarOrigin) {
                "Unexpected origin in increment/decrement container temporary variable: $tmpVarOrigin"
            }
            val expressionToIncrement = tempVariable.initializer
            val incrementedExprOrigin = (expressionToIncrement as? IrValueAccessExpression)?.origin

            val isInc = when (incrementedExprOrigin) {
                POSTFIX_INCR -> true
                POSTFIX_DECR -> false
                else -> throw IllegalStateException("Bad origin of incremented expression: $incrementedExprOrigin")
            }
            val operatorCall = DecompilerTreeIncDecOperatorCall(
                expressionToIncrement.type.buildType(),
                expressionToIncrement.buildExpression(),
                true,
                isInc
            )
            return DecompilerTreeIncDecOperatorsContainer(type.buildType(), operatorCall)
        }

        private fun IrContainerExpression.buildPrefixIncDecOperatorCallContainer(): DecompilerTreeIncDecOperatorsContainer {
            val setVariable = statements[0]
            val setVarOrigin = (setVariable as? IrSetVariable)?.origin

            val isInc = when (setVarOrigin) {
                PREFIX_INCR -> true
                PREFIX_DECR -> false
                else -> throw IllegalStateException("Bad origin of incremented expression: $setVarOrigin")
            }

            val getValue = statements[1] as IrGetValue

            val operatorCall = DecompilerTreeIncDecOperatorCall(
                getValue.type.buildType(),
                getValue.buildExpression(),
                false,
                isInc
            )
            return DecompilerTreeIncDecOperatorsContainer(type.buildType(), operatorCall)
        }

        override fun visitContainerExpression(
            expression: IrContainerExpression,
            data: ExtraData?
        ): AbstractDecompilerTreeContainerExpression =
            with(expression) {
                when (origin) {
                    WHEN -> buildWhenContainer()
                    FOR_LOOP -> buildForLoopContainer()
                    ELVIS -> DecompilerTreeElvisOperatorCallContainer(
                        type.buildType(),
                        DecompilerTreeElvisOperatorCallExpression(
                            type.buildType(),
                            statements[0].buildElement(null),
                            statements[1].buildElement(null)
                        )
                    )
                    SAFE_CALL -> DecompilerTreeSafeCallOperatorContainer(
                        type.buildType(),
                        DecompilerTreeSafeCallOperatorExpression(
                            type.buildType(),
                            statements[0].buildElement(null),
                            statements[1].buildElement(null)
                        )
                    )
                    POSTFIX_DECR, POSTFIX_INCR -> buildPostfixIncDecOperatorCallContainer()
                    PREFIX_DECR, PREFIX_INCR -> buildPrefixIncDecOperatorCallContainer()
                    OBJECT_LITERAL -> DecompilerTreeAnonymousObjectContainer(
                        type.buildType(),
                        DecompilerTreeAnonymousObject(
                            type.buildType(),
                            statements[0].buildElement(ANONYMOUS_OBJECT),
                            statements[1].buildElement(ANONYMOUS_OBJECT)
                        )
                    )
                    DESTRUCTURING_DECLARATION -> DecompilerTreeDestructingDeclarationContainer(
                        type.buildType(),
                        DecompilerTreeDestructingDeclaration(
                            type.buildType(),
                            statements[0].buildElement(data),
                            statements.drop(1).buildElements(data)
                        )
                    )
                    else -> DecompilerTreeContainerExpression(this, statements.buildElements(data), type.buildType())
                }
            }

        override fun visitStringConcatenation(expression: IrStringConcatenation, data: ExtraData?): DecompilerTreeStringConcatenation {
            val arguments = expression.arguments.buildExpressions(data)
            return DecompilerTreeStringConcatenation(expression, arguments, expression.type.buildType())
        }

        override fun visitGetObjectValue(expression: IrGetObjectValue, data: ExtraData?): DecompilerTreeGetObjectValue =
            with(expression) {
                val parent = symbol.owner.buildElement<IrClass, AbstractDecompilerTreeClass>(data)
                return DecompilerTreeGetObjectValue(this, parent, type.buildType())
            }

        override fun visitGetEnumValue(expression: IrGetEnumValue, data: ExtraData?): DecompilerTreeGetEnumValue = with(expression) {
            val parent = symbol.owner.buildElement<IrEnumEntry, DecompilerTreeEnumEntry>(data)
            return DecompilerTreeGetEnumValue(this, parent, type.buildType())
        }

        override fun visitGetValue(expression: IrGetValue, data: ExtraData?): DecompilerTreeGetValue =
            DecompilerTreeGetValue(expression, expression.type.buildType())

        override fun visitSetVariable(expression: IrSetVariable, data: ExtraData?): DecompilerTreeSetVariable = with(expression) {
            DecompilerTreeSetVariable(this, value.buildExpression(data), type.buildType())
        }

        override fun visitGetField(expression: IrGetField, data: ExtraData?): AbstractDecompilerTreeGetField = with(expression) {
            when (data) {
                CUSTOM_GETTER -> DecompilerTreeGetFieldFromGetterSetter(
                    this,
                    receiver?.buildExpression(data),
                    type.buildType()
                )
                else -> DecompilerTreeGetFieldCommon(this, receiver?.buildExpression(data), type.buildType())
            }
        }

        override fun visitSetField(expression: IrSetField, data: ExtraData?): AbstractDecompilerTreeSetField = with(expression) {
            when (data) {
                CUSTOM_SETTER -> DecompilerTreeSetFieldFromGetterSetter(
                    this,
                    receiver?.buildExpression(data),
                    value.buildExpression(data),
                    type.buildType()
                )
                else -> DecompilerTreeSetFieldCommon(this, receiver?.buildExpression(data), value.buildExpression(data), type.buildType())
            }
        }

        override fun visitCall(expression: IrCall, data: ExtraData?): AbstractDecompilerTreeCall = with(expression) {
            buildCall(
                buildDispatchReceiver(data),
                buildExtensionReceiver(data),
                buildValueArguments(data),
                type.buildType(),
                buildTypeArguments(),
                data
            )
        }

        override fun visitConstructorCall(expression: IrConstructorCall, data: ExtraData?): AbstractDecompilerTreeConstructorCall =
            with(expression) {
                val builtDispatchReceiver = buildDispatchReceiver(data)
                val builtExtensionReceiver = buildExtensionReceiver(data)
                val builtValueArguments = buildValueArguments(data)
                val builtTypeArguments = buildTypeArguments()
                when (data) {
                    ANNOTATION_CALL -> DecompilerTreeAnnotationConstructorCall(
                        this,
                        builtDispatchReceiver,
                        builtExtensionReceiver,
                        builtValueArguments,
                        type.buildType(),
                        builtTypeArguments
                    )
                    else -> DecompilerTreeCommonConstructorCall(
                        this,
                        builtDispatchReceiver,
                        builtExtensionReceiver,
                        builtValueArguments,
                        type.buildType(),
                        builtTypeArguments
                    )

                }
            }

        override fun visitDelegatingConstructorCall(
            expression: IrDelegatingConstructorCall,
            data: ExtraData?
        ): DecompilerTreeDelegatingConstructorCall = with(expression) {
            val builtDispatchReceiver = buildDispatchReceiver(data)
            val builtExtensionReceiver = buildExtensionReceiver(data)
            val builtValueArguments = buildValueArguments(data)
            val builtTypeArguments = buildTypeArguments()

            return DecompilerTreeDelegatingConstructorCall(
                this,
                builtDispatchReceiver,
                builtExtensionReceiver,
                builtValueArguments,
                type.buildType(),
                builtTypeArguments,
                symbol.owner.returnType.buildType()
            )
        }

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: ExtraData?): DecompilerTreeEnumConstructorCall =
            with(expression) {
                val builtDispatchReceiver = buildDispatchReceiver(data)
                val builtExtensionReceiver = buildExtensionReceiver(data)
                val builtValueArguments = buildValueArguments(data)

                DecompilerTreeEnumConstructorCall(
                    this,
                    builtDispatchReceiver,
                    builtExtensionReceiver,
                    builtValueArguments,
                    type.buildType()
                )
            }

        override fun visitGetClass(expression: IrGetClass, data: ExtraData?): DecompilerTreeGetClass = with(expression) {
            DecompilerTreeGetClass(this, argument.buildExpression(data), type.buildType())
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: ExtraData?): AbstractDecompilerTreeFunctionReference =
            with(expression) {
                val parentDeclaration = symbol.owner.parent.takeIf { it is IrDeclarationWithName }
                    ?.buildElement<IrDeclarationParent, DecompilerTreeDeclaration>(data)
                val buildDispatchReceiver = buildDispatchReceiver(data)
                val buildExtensionReceiver = buildExtensionReceiver(data)
                val buildValueArguments = buildValueArguments(data)
                val buildTypeArguments = buildTypeArguments()

                return when (symbol) {
                    is IrSimpleFunctionSymbol ->
                        DecompilerTreeSimpleFunctionReference(
                            this,
                            parentDeclaration,
                            buildDispatchReceiver,
                            buildExtensionReceiver,
                            buildValueArguments,
                            type.buildType(),
                            buildTypeArguments
                        )
                    is IrConstructorSymbol -> DecompilerTreeConstructorReference(
                        this,
                        parentDeclaration!!,
                        buildDispatchReceiver,
                        buildExtensionReceiver,
                        buildValueArguments,
                        type.buildType(),
                        buildTypeArguments
                    )
                    else -> throw IllegalArgumentException("Unexpected reference symbol: ${symbol.javaClass}")
                }
            }

        override fun visitPropertyReference(expression: IrPropertyReference, data: ExtraData?): DecompilerTreePropertyReference =
            with(expression) {
                DecompilerTreePropertyReference(
                    this,
                    symbol.owner.parent.takeIf { it is IrDeclarationWithName }?.buildElement(data),
                    buildDispatchReceiver(data),
                    buildExtensionReceiver(data),
                    buildValueArguments(data),
                    type.buildType(),
                    buildTypeArguments()
                )
            }

        override fun visitLocalDelegatedPropertyReference(
            expression: IrLocalDelegatedPropertyReference,
            data: ExtraData?
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

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: ExtraData?): DecompilerTreeFunctionExpression =
            with(expression) {
                DecompilerTreeFunctionExpression(this, function.buildElement(LAMBDA_CONTENT), type.buildType())
            }


        override fun visitClassReference(expression: IrClassReference, data: ExtraData?): DecompilerTreeClassReference =
            with(expression) {
                DecompilerTreeClassReference(
                    this,
                    type.buildType(),
                    classType.buildType()
                )
            }

        override fun visitInstanceInitializerCall(
            expression: IrInstanceInitializerCall,
            data: ExtraData?
        ): DecompilerTreeInstanceInitializerCall = DecompilerTreeInstanceInitializerCall(expression, expression.type.buildType())


        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: ExtraData?): DecompilerTreeTypeOperatorCall =
            with(expression) {
                DecompilerTreeTypeOperatorCall(
                    this,
                    argument.buildExpression(data),
                    type.buildType(),
                    typeOperand.buildType(),
                    WHEN_SUBJECT_MEMBER == data
                )
            }

        override fun visitWhen(expression: IrWhen, data: ExtraData?): AbstractDecompilerTreeWhen {
            val branches =
                expression.branches.buildElements<IrBranch, DecompilerTreeBranch>(data)
            return when (expression) {
                is IrIfThenElseImpl -> DecompilerTreeIfThenElse(expression, branches, expression.type.buildType())
                else -> DecompilerTreeWhen(expression, branches, expression.type.buildType())
            }
        }

        override fun visitBranch(branch: IrBranch, data: ExtraData?): AbstractDecompilerTreeBranch = with(branch) {
            val builtCondition = condition.buildExpression(data)
            val builtResult = result.buildExpression(data)

            return DecompilerTreeBranch(this, builtCondition, builtResult)
        }

        override fun visitElseBranch(branch: IrElseBranch, data: ExtraData?): DecompilerTreeElseBranch = with(branch) {
            DecompilerTreeElseBranch(branch, condition.buildExpression(data), result.buildExpression(data))
        }

        override fun visitWhileLoop(loop: IrWhileLoop, data: ExtraData?): DecompilerTreeWhileLoop = with(loop) {
            DecompilerTreeWhileLoop(this, condition.buildExpression(data), body?.buildExpression(data), type.buildType())
        }

        override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: ExtraData?): DecompilerTreeDoWhileLoop = with(loop) {
            DecompilerTreeDoWhileLoop(this, condition.buildExpression(data), body?.buildExpression(data), type.buildType())
        }

        override fun visitTry(aTry: IrTry, data: ExtraData?): DecompilerTreeTry = with(aTry) {
            DecompilerTreeTry(
                this,
                tryResult.buildExpression(data),
                catches.buildElements(data),
                finallyExpression?.buildExpression(data),
                type.buildType()
            )
        }

        override fun visitCatch(aCatch: IrCatch, data: ExtraData?): DecompilerTreeCatch {
            with(aCatch) {
                val dirCatchParameter = DecompilerTreeCatchParameterVariable(
                    catchParameter,
                    catchParameter.buildAnnotations(),
                    catchParameter.type.buildType()
                )
                val dirResult = aCatch.result.buildExpression(data)
                return DecompilerTreeCatch(this, dirCatchParameter, dirResult)
            }
        }

        override fun visitBreak(jump: IrBreak, data: ExtraData?): DecompilerTreeBreak = DecompilerTreeBreak(jump, jump.type.buildType())

        override fun visitContinue(jump: IrContinue, data: ExtraData?): DecompilerTreeContinue =
            DecompilerTreeContinue(jump, jump.type.buildType())

        override fun visitReturn(expression: IrReturn, data: ExtraData?): AbstractDecompilerTreeReturn = with(expression) {
            when (data) {
                CUSTOM_GETTER -> DecompilerTreeGetterReturn(this, value.buildExpression(data), type.buildType())
                else -> DecompilerTreeReturn(this, value.buildExpression(data), type.buildType(), data != LAMBDA_CONTENT)
            }
        }

        override fun visitThrow(expression: IrThrow, data: ExtraData?): DecompilerTreeThrow {
            val throwable = expression.value.buildExpression(data)
            return DecompilerTreeThrow(expression, throwable, expression.type.buildType())
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        @Suppress("UNCHECKED_CAST")
        fun <T : IrType, R : DecompilerTreeType> T.buildType(): R =
            (typesCacheMap[this] ?: run {
                val existingClass = elementsCacheMap.values
                    .filterIsInstance<AbstractDecompilerTreeClass>()
                    .find { it.element.defaultType == this }
                when {
                    toKotlinType().isFunctionTypeOrSubtype -> DecompilerTreeFunctionalType(this)
                    else -> DecompilerTreeSimpleType(this, existingClass)
                }.also {
                    typesCacheMap += this to it
                }
            }) as R


        @Suppress("UNCHECKED_CAST")
        fun <T : IrElement, R : DecompilerTreeElement> T.buildElement(kind: ExtraData? = null): R =
            (elementsCacheMap[this] ?: run {
                accept(this@DecompilerTreeConstructionVisitor, kind).also {
                    elementsCacheMap += this to it
                    (it as? AbstractDecompilerTreeClass)?.element?.defaultType?.also { clsType ->
                        if (typesCacheMap.containsKey(clsType)) typesCacheMap[clsType]!!.typeClassIfExists = it
                        else typesCacheMap += clsType to DecompilerTreeSimpleType(clsType, it)
                    }
                }
            }) as R

        private fun <T : IrElement, R : DecompilerTreeElement> Iterable<T>.buildElements(kind: ExtraData? = null): List<R> =
            map { it.buildElement(kind) }

        private fun IrDeclaration.buildDeclaration(kind: ExtraData? = null) =
            buildElement<IrDeclaration, DecompilerTreeDeclaration>(kind)

        private fun Iterable<IrDeclaration>.buildDeclarations(kind: ExtraData? = null) =
            map { it.buildDeclaration(kind) }

        private fun IrValueParameter.buildValueParameter(kind: ExtraData? = null) =
            buildElement<IrValueParameter, AbstractDecompilerTreeValueParameter>(kind)

        private fun Iterable<IrValueParameter>.buildValueParameters(kind: ExtraData? = null) =
            map { it.buildValueParameter(kind) }


        private fun IrExpression.buildExpression(kind: ExtraData? = null) =
            buildElement<IrExpression, DecompilerTreeExpression>(kind)

        private fun Iterable<IrExpression>.buildExpressions(kind: ExtraData? = null) =
            map { it.buildExpression(kind) }

        //TODO check inferred type correctness
        private fun IrAnnotationContainer.buildAnnotations(): List<DecompilerTreeAnnotationConstructorCall> =
            annotations.buildElements(ANNOTATION_CALL)

        private fun IrDeclarationContainer.buildDeclarations(kind: ExtraData? = null): List<DecompilerTreeDeclaration> =
            declarations.buildDeclarations(kind)

        private fun IrMemberAccessExpression<*>.buildValueArguments(kind: ExtraData? = null): List<DecompilerTreeValueArgument> {
            if (valueArgumentsCount == 0) return emptyList()

            val valueParameters = (symbol.owner as IrFunction).valueParameters
            val valueArguments = (0 until valueArgumentsCount).map { getValueArgument(it) }
            check(valueParameters.size == valueArgumentsCount) { "Number of parameters & arguments are not the same!" }
            return valueParameters.zip(valueArguments).map {
                DecompilerTreeValueArgument(
                    type.buildType(),
                    it.first.buildValueParameter(kind),
                    it.second?.buildExpression(kind),
                    null in valueArguments
                )
            }
        }

        private fun IrMemberAccessExpression<*>.buildTypeArguments() =
            (0 until typeArgumentsCount).mapNotNull { getTypeArgument(it)?.buildType() }

        private fun IrMemberAccessExpression<*>.buildDispatchReceiver(kind: ExtraData? = null): DecompilerTreeExpression? =
            dispatchReceiver?.buildExpression(kind)

        private fun IrMemberAccessExpression<*>.buildExtensionReceiver(kind: ExtraData? = null): DecompilerTreeExpression? =
            extensionReceiver?.buildExpression(kind)

        private fun IrTypeParametersContainer.buildTypeParameters(kind: ExtraData? = null): List<DecompilerTreeTypeParameter> =
            typeParameters.buildElements(kind)

        private fun IrClass.buildThisReceiver(kind: ExtraData? = null): AbstractDecompilerTreeValueParameter? =
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