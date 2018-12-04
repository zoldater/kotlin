/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection
import org.jetbrains.kotlin.idea.intentions.ConvertReceiverToParameterIntention
import org.jetbrains.kotlin.idea.intentions.MoveMemberToCompanionObjectIntention
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getStatementParentIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType

class SuspendFunctionOnCoroutineScopeInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return namedFunctionVisitor(fun(function: KtNamedFunction) {
            if (!function.hasModifier(KtTokens.SUSPEND_KEYWORD)) return
            if (!function.hasBody()) return

            val descriptor = function.resolveToDescriptorIfAny() ?: return
            val (extensionOfCoroutineScope, memberOfCoroutineScope) = with(descriptor) {
                extensionReceiverParameter.ofCoroutineScopeType() to dispatchReceiverParameter.ofCoroutineScopeType()
            }
            if (!extensionOfCoroutineScope && !memberOfCoroutineScope) return
            val context = function.analyzeWithContent()

            fun DeclarationDescriptor.isCoroutineScopeReceiver(): Boolean {
                if (extensionOfCoroutineScope && this == descriptor) return true
                if (memberOfCoroutineScope && this == descriptor.containingDeclaration) return true
                return false
            }

            fun checkSuspiciousReceiver(receiver: ReceiverValue, problemExpression: KtExpression) {
                when (receiver) {
                    is ImplicitReceiver -> if (!receiver.declarationDescriptor.isCoroutineScopeReceiver()) return
                    is ExpressionReceiver -> {
                        val receiverThisExpression = receiver.expression as? KtThisExpression ?: return
                        if (receiverThisExpression.getTargetLabel() != null) {
                            val instanceReference = receiverThisExpression.instanceReference
                            if (context[BindingContext.REFERENCE_TARGET, instanceReference]?.isCoroutineScopeReceiver() != true) return
                        }
                    }
                }
                val fixes = mutableListOf<LocalQuickFix>()
                val parentToWrap = problemExpression.getStatementParentIfAny(context)
                if (parentToWrap != null && parentToWrap !is KtDeclaration) {
                    val reportElement = (problemExpression as? KtCallExpression)?.calleeExpression ?: problemExpression
                    holder.registerProblem(
                        reportElement,
                        "Ambiguous use of CoroutineScope receiver inside suspend function",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        WrapWithCoroutineScopeFix(removeReceiver = false, wrapCallOnly = true)
                    )
                }
                fixes += WrapWithCoroutineScopeFix(removeReceiver = extensionOfCoroutineScope, wrapCallOnly = false)
                val file = function.containingKtFile
                if (extensionOfCoroutineScope) {
                    fixes += IntentionWrapper(ConvertReceiverToParameterIntention(), file)
                }
                if (memberOfCoroutineScope) {
                    val containingDeclaration = function.containingClassOrObject
                    if (containingDeclaration is KtClass && !containingDeclaration.isInterface() && function.hasBody()) {
                        fixes += IntentionWrapper(MoveMemberToCompanionObjectIntention(), file)
                    }
                }

                holder.registerProblem(
                    with(function) { receiverTypeReference ?: nameIdentifier ?: funKeyword ?: this },
                    "Ambiguous use of CoroutineScope receiver inside suspend function",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    *fixes.toTypedArray()
                )
            }

            function.forEachDescendantOfType(fun(callExpression: KtCallExpression) {
                val resolvedCall = callExpression.getResolvedCall(context) ?: return
                val extensionReceiverParameter = resolvedCall.resultingDescriptor.extensionReceiverParameter ?: return
                if (!extensionReceiverParameter.type.isCoroutineScope()) return
                val extensionReceiver = resolvedCall.extensionReceiver ?: return
                checkSuspiciousReceiver(extensionReceiver, callExpression)
            })
            function.forEachDescendantOfType(fun(nameReferenceExpression: KtNameReferenceExpression) {
                if (nameReferenceExpression.getReferencedName() != COROUTINE_CONTEXT) return
                val resolvedCall = nameReferenceExpression.getResolvedCall(context) ?: return
                if (resolvedCall.resultingDescriptor.fqNameSafe.asString() == "$COROUTINE_SCOPE.$COROUTINE_CONTEXT") {
                    val dispatchReceiver = resolvedCall.dispatchReceiver ?: return
                    checkSuspiciousReceiver(dispatchReceiver, nameReferenceExpression)
                }
            })
        })
    }

    private class WrapWithCoroutineScopeFix(
        private val removeReceiver: Boolean,
        private val wrapCallOnly: Boolean
    ) : LocalQuickFix {
        override fun getFamilyName(): String = "Wrap with coroutineScope"

        override fun getName(): String =
            when {
                removeReceiver && !wrapCallOnly -> "Remove receiver & wrap with 'coroutineScope { ... }'"
                wrapCallOnly -> "Wrap call with 'coroutineScope { ... }'"
                else -> "Wrap function body with 'coroutineScope { ... }'"
            }

        override fun startInWriteAction() = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val problemElement = descriptor.psiElement ?: return
            val function = problemElement.getNonStrictParentOfType<KtNamedFunction>() ?: return
            val functionDescriptor = function.resolveToDescriptorIfAny()
            if (!FileModificationService.getInstance().preparePsiElementForWrite(function)) return
            val bodyExpression = function.bodyExpression
            val expressionToWrap = when {
                wrapCallOnly -> (problemElement as? KtExpression)?.getStatementParentIfAny(function.analyzeWithContent())
                else -> bodyExpression
            } ?: return
            if (functionDescriptor?.extensionReceiverParameter.ofCoroutineScopeType()) {
                val context = function.analyzeWithContent()
                expressionToWrap.forEachDescendantOfType<KtDotQualifiedExpression> {
                    val receiverExpression = it.receiverExpression as? KtThisExpression
                    val selectorExpression = it.selectorExpression
                    if (receiverExpression?.getTargetLabel() != null && selectorExpression != null) {
                        if (context[BindingContext.REFERENCE_TARGET, receiverExpression.instanceReference] == functionDescriptor) {
                            it.replace(selectorExpression)
                        }
                    }
                }
            }

            val factory = KtPsiFactory(function)
            val blockExpression = function.bodyBlockExpression
            project.executeWriteCommand(name, this) {
                val result = when {
                    expressionToWrap != bodyExpression -> expressionToWrap.replaced(
                        factory.createExpressionByPattern("$COROUTINE_SCOPE_WRAPPER { $0 }", expressionToWrap)
                    )
                    blockExpression == null -> bodyExpression.replaced(
                        factory.createExpressionByPattern("$COROUTINE_SCOPE_WRAPPER { $0 }", bodyExpression)
                    )
                    else -> {
                        val bodyText = buildString {
                            for (statement in blockExpression.statements) {
                                append(statement.text)
                            }
                        }
                        blockExpression.replaced(
                            factory.createBlock("$COROUTINE_SCOPE_WRAPPER { $bodyText }")
                        )
                    }
                }
                val reformatted = CodeStyleManager.getInstance(project).reformat(result)
                ShortenReferences.DEFAULT.process(reformatted as KtElement)
            }

            val receiverTypeReference = function.receiverTypeReference
            if (removeReceiver && !wrapCallOnly && receiverTypeReference != null) {
                UnusedReceiverParameterInspection.RemoveReceiverFix.apply(receiverTypeReference, project)
            }
        }
    }

    companion object {
        private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"

        private const val COROUTINE_SCOPE_WRAPPER = "kotlinx.coroutines.coroutineScope"

        private const val COROUTINE_CONTEXT = "coroutineContext"

        private fun KotlinType.isCoroutineScope(): Boolean =
            constructor.declarationDescriptor?.fqNameSafe?.asString() == COROUTINE_SCOPE

        private fun ReceiverParameterDescriptor?.ofCoroutineScopeType(): Boolean {
            if (this == null) return false
            if (type.isCoroutineScope()) return true
            return type.constructor.supertypes.reversed().any { it.isCoroutineScope() }
        }
    }
}