/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.builder

import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.decompiler.tree.expressions.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class DecompilerTreeCreator {

    internal class DecompilerTreeConstructionVisitor : IrElementVisitor<DecompilerTreeElement, Nothing?> {
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

        override fun visitClass(declaration: IrClass, data: Nothing?): AbstractDecompilerTreeClass = declaration.constructClass()

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): DecompilerTreeSimpleFunction = with(declaration) {
            DecompilerTreeSimpleFunction(
                this, decompiledAnnotations, returnType.toDecompilerTreeType(),
                dispatchReceiverParameter?.buildValueParameter(),
                extensionReceiverParameter?.buildValueParameter(),
                valueParameters.buildValueParameters(),
                body?.buildElement()//TODO is it necessary to implicitly declare <IrBody, DecompilerTreeBody>?
            )
        }

        override fun visitConstructor(declaration: IrConstructor, data: Nothing?): DecompilerTreeConstructor {
        }

        override fun visitProperty(declaration: IrProperty, data: Nothing?): DecompilerTreeProperty {
        }

        override fun visitField(declaration: IrField, data: Nothing?): DecompilerTreeField {
        }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): DecompilerTreeElement {
        }

        override fun visitVariable(declaration: IrVariable, data: Nothing?): AbstractDecompilerTreeVariable = with(declaration) {
            DecompilerTreeVariable(this, decompiledAnnotations, type.toDecompilerTreeType(), initializer?.buildExpression())
        }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): DecompilerTreeEnumEntry = with(declaration) {
            val body = initializerExpression?.buildElement<IrExpressionBody, DecompilerTreeExpressionBody>()
            DecompilerTreeEnumEntry(this, decompiledAnnotations, body)
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): DecompilerTreeAnonymousInitializer =
            with(declaration) {
                DecompilerTreeAnonymousInitializer(this, body.buildElement())
            }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): DecompilerTreeElement {
            return super.visitTypeParameter(declaration, data)
        }

        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): DecompilerTreeElement = with(declaration) {
            DecompilerTreeValueParameter(
                this,
                decompiledAnnotations,
                //TODO calculate annotation target
                null,
                type.toDecompilerTreeType(),
                varargElementType?.toDecompilerTreeType(),
                defaultValue?.buildElement()
            )
        }

        override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): DecompilerTreeTypeAlias = with(declaration) {
            DecompilerTreeTypeAlias(this, decompiledAnnotations, aliasedType = expandedType.toDecompilerTreeType())
        }

        override fun visitBody(body: IrBody, data: Nothing?): DecompilerTreeBody {
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): DecompilerTreeExpressionBody {
        }

        override fun visitBlockBody(body: IrBlockBody, data: Nothing?): DecompilerTreeBlockBody {
        }

        override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): DecompilerTreeSyntheticBody {
        }

        override fun visitExpression(expression: IrExpression, data: Nothing?): DecompilerTreeExpression {
            TODO("Expression $expression was not properly built")
        }

        override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): DecompilerTreeConst = DecompilerTreeConst(expression)

        override fun visitVararg(expression: IrVararg, data: Nothing?): DecompilerTreeVararg = with(expression) {
            return DecompilerTreeVararg(this, elements.buildElements<IrVarargElement, DecompilerTreeVarargElement>())
        }

        override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): DecompilerTreeSpread = with(spread) {
            val decompilerIrExpression = expression.buildExpression()
            DecompilerTreeSpread(this, decompilerIrExpression)
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: Nothing?): DecompilerTreeContainerExpression =
            with(expression) {
                return DecompilerTreeContainerExpression(this, statements.buildElements())
            }

        override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): DecompilerTreeStringConcatenation {
            val arguments = expression.arguments.buildExpressions()
            return DecompilerTreeStringConcatenation(expression, arguments)
        }

        override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): DecompilerTreeGetObjectValue = with(expression) {
            val parent = symbol.owner.constructClass()
            return DecompilerTreeGetObjectValue(this, parent)
        }

        override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): DecompilerTreeGetEnumValue = with(expression) {
            val parent = symbol.owner.buildElement<IrEnumEntry, DecompilerTreeEnumEntry>()
            return DecompilerTreeGetEnumValue(this, parent)
        }

        override fun visitGetValue(expression: IrGetValue, data: Nothing?): DecompilerTreeGetValue = DecompilerTreeGetValue(expression)

        override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): DecompilerTreeSetVariable = with(expression) {
            DecompilerTreeSetVariable(this, value.buildExpression())
        }

        override fun visitGetField(expression: IrGetField, data: Nothing?): DecompilerTreeGetField = with(expression) {
            DecompilerTreeGetField(this, receiver?.buildExpression())
        }

        override fun visitSetField(expression: IrSetField, data: Nothing?): DecompilerTreeSetField = with(expression) {
            DecompilerTreeSetField(this, receiver?.buildExpression(), value.buildExpression())
        }

        override fun visitWhen(expression: IrWhen, data: Nothing?): DecompilerTreeWhen {
            val branches =
                expression.branches.map { it.accept(this, data) as DecompilerTreeBranch }
            return DecompilerTreeWhen(expression, branches)
        }

        override fun visitBranch(branch: IrBranch, data: Nothing?): DecompilerTreeBranch {
            val condition = branch.condition.buildExpression()
            val result = branch.result.buildExpression()
            return when (branch) {
                is IrElseBranch -> DecompilerTreeElseBranch(branch, condition, result)
                else -> DecompilerTreeBranchRegular(branch, condition, result)
            }
        }

        override fun visitReturn(expression: IrReturn, data: Nothing?): DecompilerTreeReturn {
            val value = expression.value.buildExpression()
            return DecompilerTreeReturn(expression, value)
        }

        override fun visitThrow(expression: IrThrow, data: Nothing?): DecompilerTreeThrow {
            val throwable =
                expression.value.buildExpression()
            return DecompilerTreeThrow(expression, throwable)
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): DecompilerTreeFunctionExpression =
            with(expression) {
                val dirFunction = function.buildElement<IrSimpleFunction, DecompilerTreeSimpleFunction>()
                DecompilerTreeFunctionExpression(this, dirFunction)
            }


        override fun visitCatch(aCatch: IrCatch, data: Nothing?): DecompilerTreeCatch {
            with(aCatch) {
                val dirCatchParameter = DecompilerTreeCatchParameterVariable(
                    catchParameter,
                    catchParameter.decompiledAnnotations,
                    catchParameter.type.toDecompilerTreeType()
                )
                val dirResult = aCatch.result.buildExpression()
                return DecompilerTreeCatch(this, dirCatchParameter, dirResult)
            }
        }


        @Suppress("UNCHECKED_CAST")
        internal fun <T : IrElement, R : DecompilerTreeElement> T.buildElement(): R =
            accept(this@DecompilerTreeConstructionVisitor, null) as R

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

        private fun IrClass.constructClass(): AbstractDecompilerTreeClass = when (kind) {
            ClassKind.INTERFACE -> DecompilerTreeInterface(this, decompiledDeclarations, decompiledAnnotations)
            ClassKind.ANNOTATION_CLASS -> DecompilerTreeAnnotationClass(this, decompiledDeclarations, decompiledAnnotations)
            ClassKind.ENUM_CLASS -> DecompilerTreeEnumClass(this, decompiledDeclarations, decompiledAnnotations)
            //TODO is it enough for `object SomeObj` val x = object : Any {...}
            ClassKind.OBJECT -> DecompilerTreeObject(this, decompiledDeclarations, decompiledAnnotations)
            else -> DecompilerTreeClass(this, decompiledDeclarations, decompiledAnnotations)
        }

    }

    companion object {
        fun buildIrModule(irModuleFragment: IrModuleFragment): DecompilerTreeModule {
            DecompilerTreeConstructionVisitor().apply {
                return irModuleFragment.buildElement()
            }
        }
    }
}