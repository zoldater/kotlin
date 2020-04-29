/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor

class ConeEffectExtractor(
    private val session: FirSession,
    private val owner: FirContractDescriptionOwner,
    private val valueParameters: List<FirValueParameter>
) : FirDefaultVisitor<ConeContractDescriptionElement?, Nothing?>() {
    companion object {
        private val BOOLEAN_AND = FirContractsDslNames.id("kotlin", "Boolean", "and")
        private val BOOLEAN_OR = FirContractsDslNames.id("kotlin", "Boolean", "or")
        private val BOOLEAN_NOT = FirContractsDslNames.id("kotlin", "Boolean", "not")
    }

    override fun visitElement(element: FirElement, data: Nothing?): ConeContractDescriptionElement? {
        return null
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Nothing?): ConeContractDescriptionElement? {
        return returnExpression.result.accept(this, data)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?): ConeContractDescriptionElement? {
        val resolvedId = functionCall.toResolvedCallableSymbol()?.callableId ?: return null
        return when (resolvedId) {
            FirContractsDslNames.IMPLIES -> {
                val effect = functionCall.explicitReceiver?.accept(this, null) as? ConeEffectDeclaration
                    ?: return null
                val condition = functionCall.argument.accept(this, null) as? ConeBooleanExpression
                    ?: return null
                ConeConditionalEffectDeclaration(effect, condition)
            }

            FirContractsDslNames.RETURNS -> {
                val argument = functionCall.arguments.firstOrNull()
                val value = if (argument == null) {
                    ConeConstantReference.WILDCARD
                } else {
                    argument.accept(this, null) as? ConeConstantReference
                        ?: return null
                }
                ConeReturnsEffectDeclaration(value)
            }

            FirContractsDslNames.RETURNS_NOT_NULL -> {
                ConeReturnsEffectDeclaration(ConeConstantReference.NULL)
            }

            FirContractsDslNames.CALLS_IN_PLACE -> {
                val reference = functionCall.arguments[0].accept(this, null) as? ConeValueParameterReference
                    ?: return null
                val kind = functionCall.arguments.getOrNull(1)?.parseInvocationKind() ?: InvocationKind.UNKNOWN
                ConeCallsEffectDeclaration(reference, kind)
            }

            BOOLEAN_AND, BOOLEAN_OR -> {
                val left = functionCall.explicitReceiver?.accept(this, null) as? ConeBooleanExpression ?: return null
                val right = functionCall.argument.accept(this, null) as? ConeBooleanExpression ?: return null
                val kind = when (resolvedId) {
                    BOOLEAN_AND -> LogicOperationKind.AND
                    BOOLEAN_OR -> LogicOperationKind.OR
                    else -> throw IllegalStateException()
                }
                ConeBinaryLogicExpression(left, right, kind)
            }

            BOOLEAN_NOT -> {
                val arg = functionCall.explicitReceiver?.accept(this, null) as? ConeBooleanExpression ?: return null
                ConeLogicalNot(arg)
            }
            else -> null
        }
    }

    override fun visitBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: Nothing?
    ): ConeContractDescriptionElement? {
        val left = binaryLogicExpression.leftOperand.accept(this, null) as? ConeBooleanExpression ?: return null
        val right = binaryLogicExpression.rightOperand.accept(this, null) as? ConeBooleanExpression ?: return null
        return ConeBinaryLogicExpression(left, right, binaryLogicExpression.kind)
    }

    override fun visitOperatorCall(operatorCall: FirOperatorCall, data: Nothing?): ConeContractDescriptionElement? {
        val isNegated = when (operatorCall.operation) {
            FirOperation.EQ -> false
            FirOperation.NOT_EQ -> true
            else -> return null
        }
        val const = operatorCall.arguments[1] as? FirConstExpression<*> ?: return null
        if (const.kind != FirConstKind.Null) return null
        val arg = operatorCall.arguments[0].accept(this, null) as? ConeValueParameterReference ?: return null
        return ConeIsNullPredicate(arg, isNegated)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Nothing?
    ): ConeContractDescriptionElement? {
        val symbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return null
        val parameter = symbol.fir as? FirValueParameter ?: return null
        val index = valueParameters.indexOf(parameter).takeUnless { it < 0 } ?: return null
        val type = parameter.returnTypeRef.coneTypeUnsafe<ConeKotlinType>()

        val name = parameter.name.asString()
        return toValueParameterReference(type, index, name)
    }

    private fun toValueParameterReference(
        type: ConeKotlinType,
        index: Int,
        name: String
    ): ConeValueParameterReference {
        return if (type == session.builtinTypes.booleanType.type) {
            ConeBooleanValueParameterReference(index, name)
        } else {
            ConeValueParameterReference(index, name)
        }
    }

    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: Nothing?
    ): ConeContractDescriptionElement? {
        val declaration = thisReceiverExpression.calleeReference.boundSymbol?.fir ?: return null
        return if (declaration == owner) {
            val type = thisReceiverExpression.typeRef.coneTypeSafe<ConeKotlinType>() ?: return null
            toValueParameterReference(type, -1, "this")
        } else {
            null
        }
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Nothing?): ConeContractDescriptionElement? {
        return when (constExpression.kind) {
            FirConstKind.Null -> ConeConstantReference.NULL
            FirConstKind.Boolean -> when (constExpression.value as Boolean) {
                true -> ConeBooleanConstantReference.TRUE
                false -> ConeBooleanConstantReference.FALSE
            }
            else -> null
        }
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): ConeContractDescriptionElement? {
        val arg = typeOperatorCall.argument.accept(this, data) as? ConeValueParameterReference ?: return null
        val type = typeOperatorCall.conversionTypeRef.coneTypeSafe<ConeKotlinType>() ?: return null
        val isNegated = typeOperatorCall.operation == FirOperation.NOT_IS
        return ConeIsInstancePredicate(arg, type, isNegated)
    }

    private fun FirExpression.parseInvocationKind(): InvocationKind? {
        if (this !is FirQualifiedAccessExpression) return null
        val resolvedId = toResolvedCallableSymbol()?.callableId ?: return null
        return when (resolvedId) {
            FirContractsDslNames.EXACTLY_ONCE_KIND -> InvocationKind.EXACTLY_ONCE
            FirContractsDslNames.AT_LEAST_ONCE_KIND -> InvocationKind.AT_LEAST_ONCE
            FirContractsDslNames.AT_MOST_ONCE_KIND -> InvocationKind.AT_MOST_ONCE
            FirContractsDslNames.UNKNOWN_KIND -> InvocationKind.UNKNOWN
            else -> null
        }
    }
}