/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.builder

import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.branch.DecompilerTreeBranch
import org.jetbrains.kotlin.decompiler.tree.branch.DecompilerTreeBranchRegular
import org.jetbrains.kotlin.decompiler.tree.branch.DecompilerTreeElseBranch
import org.jetbrains.kotlin.decompiler.tree.declarations.*
import org.jetbrains.kotlin.decompiler.tree.expressions.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class DecompilerTreeConstructor {

    internal class DecompilerTreeConstructionVisitor : IrElementVisitor<DecompilerTreeElement, Nothing?> {
        override fun visitElement(element: IrElement, data: Nothing?): DecompilerTreeElement {
            TODO("Element $element was not properly handled")
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : IrElement, R : DecompilerTreeElement> T.buildElement(): R =
            accept(this@DecompilerTreeConstructionVisitor, null) as R

        private fun <T : IrElement, R : DecompilerTreeElement> Iterable<T>.buildElements(): List<R> =
            map { it.buildElement() }

        private fun IrDeclaration.buildDeclaration() =
            buildElement<IrDeclaration, DecompilerTreeDeclaration>()

        private fun Iterable<IrDeclaration>.buildDeclarations() =
            map { it.buildDeclaration() }

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

        override fun visitClass(declaration: IrClass, data: Nothing?): AbstractDecompilerTreeClass = declaration.constructClass()

        override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): DecompilerTreeModule {
            val decompilerIrFiles =
                declaration.files.map { it.accept(this, data) as DecompilerTreeFile }
            return DecompilerTreeModule(declaration, decompilerIrFiles)
        }

        override fun visitFile(declaration: IrFile, data: Nothing?): DecompilerTreeFile = with(declaration) {
            DecompilerTreeFile(this, decompiledDeclarations, decompiledAnnotations)
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

        override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): DecompilerTreeEnumEntry = with(declaration) {
            val expression = (initializerExpression?.expression as? IrEnumConstructorCall)
                ?.buildElement<IrEnumConstructorCall, DecompilerTreeEnumConstructorCall>()
            DecompilerTreeEnumEntry(this, decompiledAnnotations, expression)
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

        override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): DecompilerTreeSpread = with(spread) {
            val decompilerIrExpression = expression.buildExpression()
            DecompilerTreeSpread(this, decompilerIrExpression)
        }


        override fun visitCatch(aCatch: IrCatch, data: Nothing?): DecompilerTreeCatch {
            with(aCatch) {
                val dirCatchParameter = DecompilerTreeCatchParameterVariable(
                    aCatch.catchParameter,
                    aCatch.catchParameter.decompiledAnnotations,
                    type = aCatch.catchParameter.type.toDecompilerTreeType()
                )
                val dirResult = aCatch.result.buildExpression()
                return DecompilerTreeCatch(this, dirCatchParameter, dirResult)
            }
        }

        override fun visitVariable(declaration: IrVariable, data: Nothing?): AbstractDecompilerTreeVariable = with(declaration) {
            DecompilerTreeVariable(this, decompiledAnnotations, type.toDecompilerTreeType(), initializer?.buildExpression())
        }

        override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): DecompilerTreeStringConcatenation {
            val arguments = expression.arguments.buildExpressions()
            return DecompilerTreeStringConcatenation(expression, arguments)
        }

        override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): DecompilerTreeConst = DecompilerTreeConst(expression)

        override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): DecompilerTreeDeclaration {
            TODO("Declaration $declaration was not properly built")
        }

        override fun visitExpression(expression: IrExpression, data: Nothing?): DecompilerTreeExpression {
            TODO("Expression $expression was not properly built")
        }

        override fun visitFunction(declaration: IrFunction, data: Nothing?): DecompilerTreeElement {
            TODO("Function $declaration was not properly built")
        }
    }

    companion object {
        fun buildIrModule(irModuleFragment: IrModuleFragment): DecompilerTreeModule {
            return irModuleFragment.accept(DecompilerTreeConstructionVisitor(), null) as DecompilerTreeModule
        }
    }
}