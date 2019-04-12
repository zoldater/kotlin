/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.LHSArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun resolveKtPrimitive(
    csBuilder: ConstraintSystemBuilder,
    argument: KotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder,
    isReceiver: Boolean
): ResolvedAtom = when (argument) {
    is SimpleKotlinCallArgument ->
        checkSimpleArgument(csBuilder, argument, expectedType, diagnosticsHolder, isReceiver)

    is LambdaKotlinCallArgument ->
        preprocessLambdaArgument(csBuilder, argument, expectedType)

    is CallableReferenceKotlinCallArgument ->
        preprocessCallableReference(csBuilder, argument, expectedType, diagnosticsHolder)

    is CollectionLiteralKotlinCallArgument ->
        preprocessCollectionLiteralArgument(argument, expectedType)

    else -> unexpectedArgument(argument)
}


// if expected type isn't function type, then may be it is Function<R>, Any or just `T`
private fun preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: LambdaKotlinCallArgument,
    expectedType: UnwrappedType?,
    forceResolution: Boolean = false
): ResolvedAtom {
    if (expectedType != null && !forceResolution && csBuilder.isTypeVariable(expectedType)) {
        return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType)
    }

    fun extract() = extraLambdaInfo(expectedType, argument, csBuilder)

    fun ResolvedLambdaAtom.lambdaType() = createFunctionType(
        csBuilder.builtIns, Annotations.EMPTY, receiver,
        parameters, null, returnType, isSuspend
    )

    if (expectedType == null) {
        return extract()
    }

    fun processLambdaWithSignatureResolve(): ResolvedLambdaAtom {
        val resolvedArgument = extract()
        csBuilder.addSubtypeConstraint(resolvedArgument.lambdaType(), expectedType, ArgumentConstraintPosition(argument))
        return resolvedArgument
    }

    // Here we are trying to extract lambda info from fixed type
    val resolvedArgument = extractLambdaInfoFromFunctionalType(expectedType, argument)
        ?: return processLambdaWithSignatureResolve()

    /*
     * If lambda info from expected type doesn't match, we extract lambda info from resolving of lambda body
     * It helps with conversions from usual lambda to lambda with receiver
     *
     * Assume we have expression
     *  val x: A.() -> Unit = id { a: A -> Unit }
     *
     * Here we have lambda type from method `extractLambdaInfoFromFunctionalType` is `A.(A) -> Unit`
     * It does not math expected type `A.() -> Unit`, so constraint system has contradiction
     *
     * When we see that, we infer type of lambda using only it's signature, so it infers to `(A) -> Unit`, that
     *   matches to expected type, and everything is OK
     */
    val success = csBuilder.runTransaction {
        addSubtypeConstraint(resolvedArgument.lambdaType(), expectedType, ArgumentConstraintPosition(argument))
        !hasContradiction
    }

    if (!success) {
        return processLambdaWithSignatureResolve()
    }

    return resolvedArgument
}

private fun extraLambdaInfo(
    expectedType: UnwrappedType?,
    argument: LambdaKotlinCallArgument,
    csBuilder: ConstraintSystemBuilder
): ResolvedLambdaAtom {
    val builtIns = csBuilder.builtIns
    val isSuspend = expectedType?.isSuspendFunctionType ?: false

    val isFunctionSupertype = expectedType != null && KotlinBuiltIns.isNotNullOrNullableFunctionSupertype(expectedType)
    val argumentAsFunctionExpression = argument.safeAs<FunctionExpression>()

    val typeVariable = TypeVariableForLambdaReturnType(argument, builtIns, "_L")

    val receiverType = argumentAsFunctionExpression?.receiverType
    val returnType =
        argumentAsFunctionExpression?.returnType ?: expectedType?.arguments?.singleOrNull()?.type?.unwrap()?.takeIf { isFunctionSupertype }
        ?: typeVariable.defaultType

    val parameters = argument.parametersTypes?.map { it ?: builtIns.nothingType } ?: emptyList()

    val newTypeVariableUsed = returnType == typeVariable.defaultType
    if (newTypeVariableUsed) csBuilder.registerVariable(typeVariable)

    return ResolvedLambdaAtom(argument, isSuspend, receiverType, parameters, returnType, typeVariable.takeIf { newTypeVariableUsed })
}

private fun extractLambdaInfoFromFunctionalType(expectedType: UnwrappedType?, argument: LambdaKotlinCallArgument): ResolvedLambdaAtom? {
    if (expectedType == null || !expectedType.isBuiltinFunctionalType) return null
    val parameters = extractLambdaParameters(expectedType, argument)

    val argumentAsFunctionExpression = argument.safeAs<FunctionExpression>()
    val receiverType = argumentAsFunctionExpression?.receiverType ?: expectedType.getReceiverTypeFromFunctionType()?.unwrap()
    val returnType = argumentAsFunctionExpression?.returnType ?: expectedType.getReturnTypeFromFunctionType().unwrap()

    return ResolvedLambdaAtom(
        argument,
        expectedType.isSuspendFunctionType,
        receiverType,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = null
    )
}

private fun extractLambdaParameters(expectedType: UnwrappedType, argument: LambdaKotlinCallArgument): List<UnwrappedType> {
    val parametersTypes = argument.parametersTypes
    val expectedParameters = expectedType.getValueParameterTypesFromFunctionType()
    if (parametersTypes == null) {
        return expectedParameters.map { it.type.unwrap() }
    }

    return parametersTypes.mapIndexed { index, type ->
        type ?: expectedParameters.getOrNull(index)?.type?.unwrap() ?: expectedType.builtIns.nullableAnyType
    }
}

fun LambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(csBuilder: ConstraintSystemBuilder): ResolvedLambdaAtom {
    val fixedExpectedType = csBuilder.buildCurrentSubstitutor().safeSubstitute(expectedType)
    val resolvedLambdaAtom = preprocessLambdaArgument(csBuilder, atom, fixedExpectedType, forceResolution = true) as ResolvedLambdaAtom

    setAnalyzed(resolvedLambdaAtom)

    return resolvedLambdaAtom
}

private fun preprocessCallableReference(
    csBuilder: ConstraintSystemBuilder,
    argument: CallableReferenceKotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder
): ResolvedAtom {
    val result = EagerCallableReferenceAtom(argument, expectedType)

    if (expectedType == null) return result

    val notCallableTypeConstructor =
        csBuilder.getProperSuperTypeConstructors(expectedType)
            .firstOrNull { !ReflectionTypes.isPossibleExpectedCallableType(it.requireIs()) }

    argument.lhsResult.safeAs<LHSResult.Type>()?.let {
        val lhsType = it.unboundDetailedReceiver.stableType
        if (ReflectionTypes.isNumberedTypeWithOneOrMoreNumber(expectedType)) {
            val lhsTypeVariable = expectedType.arguments.first().type.unwrap()
            csBuilder.addSubtypeConstraint(lhsType, lhsTypeVariable, LHSArgumentConstraintPosition(it.qualifier))
        }
    }
    if (notCallableTypeConstructor != null) {
        diagnosticsHolder.addDiagnostic(
            NotCallableExpectedType(
                argument,
                expectedType,
                notCallableTypeConstructor.requireIs()
            )
        )
    }
    return result
}

private fun preprocessCollectionLiteralArgument(
    collectionLiteralArgument: CollectionLiteralKotlinCallArgument,
    expectedType: UnwrappedType?
): ResolvedAtom {
    // todo add some checks about expected type
    return ResolvedCollectionLiteralAtom(collectionLiteralArgument, expectedType)
}

internal inline fun <reified T : Any> Any.requireIs(): T {
    require(this is T)
    return this
}