/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.builder

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.decompiler.tree.expressions.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.isLocalClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class DecompilerTreeCreator {

    internal class DecompilerTreeConstructionVisitor : IrElementVisitor<DecompilerTreeElement, Nothing?> {
        private val elementsCacheMap = mutableMapOf<IrElement, DecompilerTreeElement>()
        private val typesCacheMap = mutableMapOf<IrType, DecompilerTreeType>()

        override fun visitElement(element: IrElement, data: Nothing?): DecompilerTreeElement {
            TODO("Element $element was not properly built")
        }

        override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): DecompilerTreeModule {
            val decompilerIrFiles =
                declaration.files.map { it.accept(this, data) as DecompilerTreeFile }
            return DecompilerTreeModule(declaration, decompilerIrFiles)
        }

        override fun visitPackageFragment(declaration: IrPackageFragment, data: Nothing?): DecompilerTreeElement {
            TODO("Package $declaration was not properly built")
        }

        override fun visitFile(declaration: IrFile, data: Nothing?): DecompilerTreeFile = with(declaration) {
            DecompilerTreeFile(this, decompiledDeclarations, decompiledAnnotations)
        }

        override fun visitScript(declaration: IrScript, data: Nothing?): DecompilerTreeElement {
            TODO("Script $declaration was not properly built")
        }

        override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): DecompilerTreeDeclaration {
            TODO("Declaration $declaration was not properly built")
        }

        override fun visitClass(declaration: IrClass, data: Nothing?): AbstractDecompilerTreeClass = with(declaration) {
            buildClass(this, decompiledDeclarations, decompiledAnnotations, buildTypeParameters, buildThisReceiver, buildSuperTypes)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): DecompilerTreeSimpleFunction = with(declaration) {
            DecompilerTreeSimpleFunction(
                this, decompiledAnnotations, returnType.buildType(),
                dispatchReceiverParameter?.buildValueParameter(),
                extensionReceiverParameter?.buildValueParameter(),
                valueParameters.buildValueParameters(),
                body?.buildElement(), //TODO is it necessary to implicitly declare <IrBody, DecompilerTreeBody>?
                buildTypeParameters
            )
        }

        override fun visitConstructor(declaration: IrConstructor, data: Nothing?): DecompilerTreeConstructor = with(declaration) {
            DecompilerTreeConstructor(
                this,
                decompiledAnnotations,
                returnType.buildType(),
                dispatchReceiverParameter?.buildValueParameter(),
                extensionReceiverParameter?.buildValueParameter(),
                valueParameters.buildValueParameters(),
                body?.buildElement(),
                buildTypeParameters
            )
        }

        override fun visitProperty(declaration: IrProperty, data: Nothing?): DecompilerTreeProperty = with(declaration) {
            DecompilerTreeProperty(
                this,
                decompiledAnnotations,
                backingField?.buildElement(),
                getter?.buildElement(),
                setter?.buildElement()
            )
        }

        override fun visitField(declaration: IrField, data: Nothing?): DecompilerTreeField = with(declaration) {
            DecompilerTreeField(this, decompiledAnnotations, initializer?.buildElement(), type.buildType())
        }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): DecompilerTreeElement {
            TODO("Local delegated property $declaration was not built")
        }

        override fun visitVariable(declaration: IrVariable, data: Nothing?): AbstractDecompilerTreeVariable = with(declaration) {
            DecompilerTreeVariable(this, decompiledAnnotations, initializer?.buildExpression(), type.buildType())
        }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): DecompilerTreeEnumEntry = with(declaration) {
            val body = initializerExpression?.buildElement<IrExpressionBody, DecompilerTreeExpressionBody>()
            DecompilerTreeEnumEntry(this, decompiledAnnotations, body)
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): DecompilerTreeAnonymousInitializer =
            with(declaration) {
                DecompilerTreeAnonymousInitializer(this, body.buildElement())
            }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): DecompilerTreeTypeParameter = with(declaration) {
            DecompilerTreeTypeParameter(this, decompiledAnnotations)
        }

        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): DecompilerTreeValueParameter = with(declaration) {
            DecompilerTreeValueParameter(
                this,
                decompiledAnnotations,
                //TODO calculate annotation target
                defaultValue?.buildElement(),
                type.buildType(),
                varargElementType?.buildType()
            )
        }

        override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): DecompilerTreeTypeAlias = with(declaration) {
            DecompilerTreeTypeAlias(
                this, decompiledAnnotations,
                aliasedType = expandedType.buildType(),
                typeParameters = buildTypeParameters
            )
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): DecompilerTreeExpressionBody = with(body) {
            DecompilerTreeExpressionBody(this, expression.buildExpression())
        }

        override fun visitBlockBody(body: IrBlockBody, data: Nothing?): DecompilerTreeBlockBody = with(body) {
            DecompilerTreeBlockBody(this, statements.buildElements())
        }

        override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): DecompilerTreeSyntheticBody = with(body) {
            DecompilerTreeSyntheticBody(this)
        }

        override fun visitExpression(expression: IrExpression, data: Nothing?): DecompilerTreeExpression {
            TODO("Expression $expression was not properly built")
        }

        override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): DecompilerTreeConst =
            DecompilerTreeConst(expression, expression.type.buildType())

        override fun visitVararg(expression: IrVararg, data: Nothing?): DecompilerTreeVararg = with(expression) {
            return DecompilerTreeVararg(this, elements.buildElements(), type.buildType())
        }

        override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): DecompilerTreeSpread = with(spread) {
            val decompilerIrExpression = expression.buildExpression()
            DecompilerTreeSpread(this, decompilerIrExpression)
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: Nothing?): DecompilerTreeContainerExpression =
            with(expression) {
                return DecompilerTreeContainerExpression(this, statements.buildElements(), type.buildType())
            }

        override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): DecompilerTreeStringConcatenation {
            val arguments = expression.arguments.buildExpressions()
            return DecompilerTreeStringConcatenation(expression, arguments, expression.type.buildType())
        }

        override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): DecompilerTreeGetObjectValue = with(expression) {
            val parent = symbol.owner.buildElement<IrClass, DecompilerTreeClass>()
            return DecompilerTreeGetObjectValue(this, parent, type.buildType())
        }

        override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): DecompilerTreeGetEnumValue = with(expression) {
            val parent = symbol.owner.buildElement<IrEnumEntry, DecompilerTreeEnumEntry>()
            return DecompilerTreeGetEnumValue(this, parent, type.buildType())
        }

        override fun visitGetValue(expression: IrGetValue, data: Nothing?): DecompilerTreeGetValue =
            DecompilerTreeGetValue(expression, expression.type.buildType())

        override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): DecompilerTreeSetVariable = with(expression) {
            DecompilerTreeSetVariable(this, value.buildExpression(), type.buildType())
        }

        override fun visitGetField(expression: IrGetField, data: Nothing?): DecompilerTreeGetField = with(expression) {
            DecompilerTreeGetField(this, receiver?.buildExpression(), type.buildType())
        }

        override fun visitSetField(expression: IrSetField, data: Nothing?): DecompilerTreeSetField = with(expression) {
            DecompilerTreeSetField(this, receiver?.buildExpression(), value.buildExpression(), type.buildType())
        }

        override fun visitCall(expression: IrCall, data: Nothing?): AbstractDecompilerTreeCall = with(expression) {
            buildCall(buildDispatchReceiver, buildExtensionReceiver, buildValueArguments, type.buildType())
        }

        override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): DecompilerTreeConstructorCall = with(expression) {
            DecompilerTreeConstructorCall(this, buildDispatchReceiver, buildExtensionReceiver, buildValueArguments, type.buildType())
        }

        override fun visitDelegatingConstructorCall(
            expression: IrDelegatingConstructorCall,
            data: Nothing?
        ): DecompilerTreeDelegatingConstructorCall = with(expression) {
            DecompilerTreeDelegatingConstructorCall(
                this,
                buildDispatchReceiver,
                buildExtensionReceiver,
                buildValueArguments,
                type.buildType()
            )
        }

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): DecompilerTreeEnumConstructorCall =
            with(expression) {
                DecompilerTreeEnumConstructorCall(
                    this,
                    buildDispatchReceiver,
                    buildExtensionReceiver,
                    buildValueArguments,
                    type.buildType()
                )
            }

        override fun visitGetClass(expression: IrGetClass, data: Nothing?): DecompilerTreeGetClass =
            DecompilerTreeGetClass(expression, expression.argument.buildExpression(), expression.type.buildType())

        override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): DecompilerTreeFunctionReference =
            with(expression) {
                DecompilerTreeFunctionReference(this, buildDispatchReceiver, buildExtensionReceiver, buildValueArguments, type.buildType())
            }

        override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): DecompilerTreePropertyReference =
            with(expression) {
                DecompilerTreePropertyReference(this, buildDispatchReceiver, buildExtensionReceiver, buildValueArguments, type.buildType())
            }

        override fun visitLocalDelegatedPropertyReference(
            expression: IrLocalDelegatedPropertyReference,
            data: Nothing?
        ): DecompilerTreeLocalDelegatedPropertyReference =
            with(expression) {
                DecompilerTreeLocalDelegatedPropertyReference(
                    this,
                    buildDispatchReceiver,
                    buildExtensionReceiver,
                    buildValueArguments,
                    type.buildType()
                )
            }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): DecompilerTreeFunctionExpression =
            with(expression) {
                DecompilerTreeFunctionExpression(this, function.buildElement(), type.buildType())
            }


        override fun visitClassReference(expression: IrClassReference, data: Nothing?): DecompilerTreeClassReference =
            DecompilerTreeClassReference(expression, expression.type.buildType(), expression.classType.buildType())

        override fun visitInstanceInitializerCall(
            expression: IrInstanceInitializerCall,
            data: Nothing?
        ): DecompilerTreeInstanceInitializerCall = DecompilerTreeInstanceInitializerCall(expression, expression.type.buildType())


        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): DecompilerTreeTypeOperatorCall = with(expression) {
            DecompilerTreeTypeOperatorCall(this, argument.buildExpression(), type.buildType(), typeOperand.buildType())
        }

        override fun visitWhen(expression: IrWhen, data: Nothing?): AbstractDecompilerTreeWhen {
            val branches =
                expression.branches.map { it.accept(this, data) as AbstractDecompilerTreeBranch }
            return when (expression) {
                is IrIfThenElseImpl -> DecompilerTreeIfThenElse(expression, branches, expression.type.buildType())
                else -> DecompilerTreeWhen(expression, branches, expression.type.buildType())
            }
        }

        override fun visitBranch(branch: IrBranch, data: Nothing?): AbstractDecompilerTreeBranch = with(branch) {
            DecompilerTreeBranch(branch, condition.buildExpression(), result.buildExpression())
        }

        override fun visitElseBranch(branch: IrElseBranch, data: Nothing?): DecompilerTreeElseBranch = with(branch) {
            DecompilerTreeElseBranch(branch, condition.buildExpression(), result.buildExpression())
        }

        override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): DecompilerTreeWhileLoop = with(loop) {
            DecompilerTreeWhileLoop(this, condition.buildExpression(), body?.buildExpression(), type.buildType())
        }

        override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): DecompilerTreeDoWhileLoop = with(loop) {
            DecompilerTreeDoWhileLoop(this, condition.buildExpression(), body?.buildExpression(), type.buildType())
        }

        override fun visitTry(aTry: IrTry, data: Nothing?): DecompilerTreeTry = with(aTry) {
            DecompilerTreeTry(
                this,
                tryResult.buildExpression(),
                catches.buildElements(),
                finallyExpression?.buildExpression(),
                type.buildType()
            )
        }

        override fun visitCatch(aCatch: IrCatch, data: Nothing?): DecompilerTreeCatch {
            with(aCatch) {
                val dirCatchParameter = DecompilerTreeCatchParameterVariable(
                    catchParameter,
                    catchParameter.decompiledAnnotations,
                    catchParameter.type.buildType()
                )
                val dirResult = aCatch.result.buildExpression()
                return DecompilerTreeCatch(this, dirCatchParameter, dirResult)
            }
        }

        override fun visitBreak(jump: IrBreak, data: Nothing?): DecompilerTreeBreak = DecompilerTreeBreak(jump, jump.type.buildType())

        override fun visitContinue(jump: IrContinue, data: Nothing?): DecompilerTreeContinue =
            DecompilerTreeContinue(jump, jump.type.buildType())

        override fun visitReturn(expression: IrReturn, data: Nothing?): DecompilerTreeReturn {
            val value = expression.value.buildExpression()
            return DecompilerTreeReturn(expression, value, expression.type.buildType())
        }

        override fun visitThrow(expression: IrThrow, data: Nothing?): DecompilerTreeThrow {
            val throwable = expression.value.buildExpression()
            return DecompilerTreeThrow(expression, throwable, expression.type.buildType())
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T : IrType, R : DecompilerTreeType> T.buildType(): R =
            (typesCacheMap[this] ?: run {
                when {
                    toKotlinType().isFunctionTypeOrSubtype -> DecompilerTreeFunctionalType(this)
                    classOrNull?.owner?.isLocalClass() ?: false -> DecompilerTreeLocalClassType(this)
                    else -> DecompilerTreeSimpleType(this)
                }.also { typesCacheMap += this to it }
            }) as R


        @Suppress("UNCHECKED_CAST")
        internal fun <T : IrElement, R : DecompilerTreeElement> T.buildElement(): R =
            (elementsCacheMap[this] ?: run {
                accept(this@DecompilerTreeConstructionVisitor, null).also { elementsCacheMap += this to it }
            }) as R

        private fun <T : IrElement, R : DecompilerTreeElement> Iterable<T>.buildElements(): List<R> =
            map { it.buildElement() }

        private fun IrDeclaration.buildDeclaration() =
            buildElement<IrDeclaration, DecompilerTreeDeclaration>()

        private fun Iterable<IrDeclaration>.buildDeclarations() =
            map { it.buildDeclaration() }

        private fun IrValueParameter.buildValueParameter() =
            buildElement<IrValueParameter, DecompilerTreeValueParameter>()

        private fun Iterable<IrValueParameter>.buildValueParameters() =
            map { it.buildValueParameter() }


        private fun IrExpression.buildExpression() =
            buildElement<IrExpression, DecompilerTreeExpression>()

        private fun Iterable<IrExpression>.buildExpressions() =
            map { it.buildExpression() }

        //TODO check inferred type correctness
        private val IrAnnotationContainer.decompiledAnnotations: List<DecompilerTreeConstructorCall>
            get() = annotations.buildElements()
        private val IrDeclarationContainer.decompiledDeclarations: List<DecompilerTreeDeclaration>
            get() = declarations.buildDeclarations()

        private val IrMemberAccessExpression.buildValueArguments
            get() = (0 until valueArgumentsCount).mapNotNull { getValueArgument(it) }.buildExpressions()

        private val IrMemberAccessExpression.buildDispatchReceiver: DecompilerTreeExpression?
            get() = dispatchReceiver?.buildExpression()

        private val IrMemberAccessExpression.buildExtensionReceiver: DecompilerTreeExpression?
            get() = extensionReceiver?.buildExpression()

        private val IrTypeParametersContainer.buildTypeParameters: List<DecompilerTreeTypeParameter>
            get() = typeParameters.buildElements()

        private val IrClass.buildThisReceiver: DecompilerTreeValueParameter?
            get() = thisReceiver?.buildValueParameter()

        private val IrClass.buildSuperTypes: List<DecompilerTreeType>
            get() = superTypes.map { it.buildType() }

    }

    companion object {
        fun buildIrModule(irModuleFragment: IrModuleFragment): DecompilerTreeModule {
            DecompilerTreeConstructionVisitor().apply {
                return irModuleFragment.buildElement()
            }
        }
    }
}