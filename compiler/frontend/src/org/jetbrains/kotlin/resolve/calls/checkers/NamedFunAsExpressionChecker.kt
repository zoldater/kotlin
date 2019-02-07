/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors.ANONYMOUS_FUNCTION_WITH_NAME
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.isFunctionalExpression
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object NamedFunAsExpressionChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) return
        for (argument in resolvedCall.valueArguments.values.filterIsInstance(ExpressionValueArgument::class.java)) {
            val expression =
                argument.valueArgument?.getArgumentExpression().safeAs<KtPsiUtil.KtExpressionWrapper>()?.baseExpression ?: continue
            if (expression is KtNamedFunction && !expression.isFunctionalExpression()) {
                context.trace.report(ANONYMOUS_FUNCTION_WITH_NAME.on(expression.nameIdentifier!!))
            }
        }
    }
}