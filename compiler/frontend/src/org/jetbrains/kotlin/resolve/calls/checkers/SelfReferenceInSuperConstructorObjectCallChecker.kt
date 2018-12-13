/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class SelfReferenceInSuperConstructorObjectCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val call = resolvedCall.call.callElement
        if (call !is KtSuperTypeCallEntry) return

        val callDescriptor = resolvedCall.resultingDescriptor
        if (callDescriptor !is ClassConstructorDescriptor) return

        val declaration = call.parent.parent
        if (declaration !is KtObjectDeclaration) return

        val objectDescriptor = declaration.findClassDescriptor(context.trace.bindingContext)
        if (objectDescriptor.kind != ClassKind.OBJECT) return

        val objectThis = objectDescriptor.thisAsReceiverParameter.value

        for ((_, resolvedArgument) in resolvedCall.valueArguments) {
            for (argument in resolvedArgument.arguments) {
                val argumentExpression = argument.getArgumentExpression()
                val argumentResolvedCall = argumentExpression?.getResolvedCall(context.trace.bindingContext) ?: continue
                if (objectThis.type == argumentResolvedCall.dispatchReceiver?.type ||
                    objectThis.type == argumentResolvedCall.resultingDescriptor.returnType
                ) {
                    context.reportDiagnostic(argumentExpression)
                }
            }
        }
    }

    private fun CallCheckerContext.reportDiagnostic(reportOn: KtExpression) {
        val diagnostic =
            if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitSelfReferenceInSuperConstructorCallOfObject))
                Errors.SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT
            else
                Errors.SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING

        trace.report(diagnostic.on(reportOn))
    }
}

