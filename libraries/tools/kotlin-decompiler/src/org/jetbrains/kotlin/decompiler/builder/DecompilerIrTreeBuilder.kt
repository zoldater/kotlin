/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.builder

import org.jetbrains.kotlin.decompiler.tree.*
import org.jetbrains.kotlin.decompiler.tree.branch.DecompilerIrBranch
import org.jetbrains.kotlin.decompiler.tree.branch.DecompilerIrBranchRegular
import org.jetbrains.kotlin.decompiler.tree.branch.DecompilerIrElseBranch
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerIrDeclaration
import org.jetbrains.kotlin.decompiler.tree.declarations.DecompilerIrVariable
import org.jetbrains.kotlin.decompiler.tree.declarations.functions.DecompilerIrSimpleFunction
import org.jetbrains.kotlin.decompiler.tree.expressions.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class DecompilerIrTreeBuilder {

    private class DecompilerIrTreeBuilderVisitor : IrElementVisitor<DecompilerIrElement<out IrElement>, Nothing?> {
        override fun visitElement(element: IrElement, data: Nothing?): DecompilerIrElement<*> {
            TODO("Not yet implemented")
        }

        private inline fun <reified T : IrStatement, reified R : DecompilerIrStatement> T.buildStatement(): R =
            accept(this@DecompilerIrTreeBuilderVisitor, null) as R

        private inline fun <reified T : IrStatement, reified R : DecompilerIrStatement> Iterable<T>.buildStatements(): List<R> =
            map { it.buildStatement() }

        private fun IrDeclaration.buildDeclaration() =
            buildStatement<IrDeclaration, DecompilerIrDeclaration>()

        private fun Iterable<IrDeclaration>.buildDeclarations() =
            map { it.buildDeclaration() }

        private fun IrExpression.buildExpression() =
            buildStatement<IrExpression, DecompilerIrExpression>()

        private fun Iterable<IrExpression>.buildExpressions() =
            map { it.buildExpression() }

        override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): DecompilerIrModule {
            val decompilerIrFiles =
                declaration.files.map { it.accept(this, data) as DecompilerIrFile }
            return DecompilerIrModule(declaration, decompilerIrFiles)
        }

        override fun visitFile(declaration: IrFile, data: Nothing?): DecompilerIrFile {
            val declarations = declaration.declarations.buildDeclarations()
            return DecompilerIrFile(declaration, declarations)
        }

        override fun visitWhen(expression: IrWhen, data: Nothing?): DecompilerIrWhen {
            val branches =
                expression.branches.map { it.accept(this, data) as DecompilerIrBranch }
            return DecompilerIrWhen(expression, branches)
        }

        override fun visitBranch(branch: IrBranch, data: Nothing?): DecompilerIrBranch {
            val dirCondition = branch.condition.buildExpression()
            val dirResult = branch.result.buildExpression()
            return when (branch) {
                is IrElseBranch -> DecompilerIrElseBranch(branch, dirCondition, dirResult)
                else -> DecompilerIrBranchRegular(branch, dirCondition, dirResult)
            }
        }

        override fun visitReturn(expression: IrReturn, data: Nothing?): DecompilerIrReturn {
            val dirValue = expression.value.buildExpression()
            return DecompilerIrReturn(expression, dirValue)
        }

        override fun visitThrow(expression: IrThrow, data: Nothing?): DecompilerIrThrow {
            val throwable =
                expression.value.buildExpression()
            return DecompilerIrThrow(expression, throwable)
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): DecompilerIrFunctionExpression {
            val dirFunction = expression.function.buildDeclaration() as DecompilerIrSimpleFunction
            return DecompilerIrFunctionExpression(expression, dirFunction)
        }

        override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): DecompilerIrSpread {
            with(spread) {
                val decompilerIrExpression = expression.buildExpression()
                return DecompilerIrSpread(this, decompilerIrExpression)
            }
        }

        override fun visitCatch(aCatch: IrCatch, data: Nothing?): DecompilerIrCatch {
            with(aCatch) {
                val dirCatchParameter = catchParameter.buildStatement<IrVariable, DecompilerIrVariable>()
                val dirResult = aCatch.result.buildExpression()
                return DecompilerIrCatch(this, dirCatchParameter, dirResult)
            }
        }

        override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): DecompilerIrConst = DecompilerIrConst(expression)
    }

    companion object {
        fun buildIrModule(irModuleFragment: IrModuleFragment): DecompilerIrModule {
            return irModuleFragment.accept(DecompilerIrTreeBuilderVisitor(), null) as DecompilerIrModule
        }
    }
}