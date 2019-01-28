/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.ResolveConstruct.TRY

internal fun ControlStructureTypingUtils.resolveTryAsCallImpl(
    call: Call,
    tryExpression: KtTryExpression,
    catchedExceptions: List<Pair<KtExpression, VariableDescriptor>>,
    context: ExpressionTypingContext,
    dataFlowInfoForArguments: MutableDataFlowInfoForArguments?
): ResolvedCall<FunctionDescriptor> {
    val argumentNames = listOf("tryBlock") + tryExpression.catchClauses.mapIndexed { index, _ -> "catchBlock$index"}
    val isNullable = argumentNames.map { false }
    val function = createFunctionDescriptorForSpecialConstruction(TRY, argumentNames, isNullable)
    for ((expression, variableDescriptor) in catchedExceptions) {
        context.trace.record(BindingContext.NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER, expression, variableDescriptor)
    }
    return resolveSpecialConstructionAsCall(call, function, TRY, context, dataFlowInfoForArguments)
}