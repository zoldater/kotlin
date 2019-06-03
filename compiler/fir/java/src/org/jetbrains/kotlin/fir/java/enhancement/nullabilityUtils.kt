/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.resolvedFqName
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus
import org.jetbrains.kotlin.utils.Jsr305State
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

fun List<FirAnnotationCall>.extractNullability(
    annotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver,
    jsr305State: Jsr305State
): NullabilityQualifierWithMigrationStatus? =
    this.firstNotNullResult { annotationCall ->
        annotationCall.extractNullability(annotationTypeQualifierResolver, jsr305State)
    }


fun FirAnnotationCall.extractNullability(
    annotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver,
    jsr305State: Jsr305State
): NullabilityQualifierWithMigrationStatus? {
    this.extractNullabilityFromKnownAnnotations(jsr305State)?.let { return it }

    val typeQualifierAnnotation =
        annotationTypeQualifierResolver.resolveTypeQualifierAnnotation(this)
            ?: return null

    val jsr305ReportLevel = annotationTypeQualifierResolver.resolveJsr305ReportLevel(this)
    if (jsr305ReportLevel.isIgnore) return null

    return typeQualifierAnnotation.extractNullabilityFromKnownAnnotations(jsr305State)?.copy(isForWarningOnly = jsr305ReportLevel.isWarning)
}

private val NULLABLE_ANNOTATIONS_AS_STRING = NULLABLE_ANNOTATIONS.map { it.asString() }

private val NOT_NULL_ANNOTATIONS_AS_STRING = NOT_NULL_ANNOTATIONS.map { it.asString() }

private fun FirAnnotationCall.extractNullabilityFromKnownAnnotations(jsr305State: Jsr305State): NullabilityQualifierWithMigrationStatus? {
    val annotationFqName = resolvedFqName ?: return null

    val annotationFqNameAsString = annotationFqName.toString()
    return when {
        annotationFqNameAsString in NULLABLE_ANNOTATIONS_AS_STRING -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)
        annotationFqNameAsString in NOT_NULL_ANNOTATIONS_AS_STRING -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)
        annotationFqNameAsString == JAVAX_NONNULL_ANNOTATION.asString() -> extractNullabilityTypeFromArgument()

        annotationFqNameAsString == COMPATQUAL_NULLABLE_ANNOTATION.asString() && jsr305State.enableCompatqualCheckerFrameworkAnnotations ->
            NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)

        annotationFqNameAsString == COMPATQUAL_NONNULL_ANNOTATION.asString() && jsr305State.enableCompatqualCheckerFrameworkAnnotations ->
            NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)

        annotationFqNameAsString == ANDROIDX_RECENTLY_NON_NULL_ANNOTATION.asString() -> NullabilityQualifierWithMigrationStatus(
            NullabilityQualifier.NOT_NULL,
            isForWarningOnly = true
        )

        annotationFqNameAsString == ANDROIDX_RECENTLY_NULLABLE_ANNOTATION.asString() -> NullabilityQualifierWithMigrationStatus(
            NullabilityQualifier.NULLABLE,
            isForWarningOnly = true
        )
        else -> null
    }
}

private fun FirAnnotationCall.extractNullabilityTypeFromArgument(): NullabilityQualifierWithMigrationStatus? {
    val enumValue = this.arguments.firstOrNull()?.toResolvedCallableSymbol()?.callableId?.callableName
    // if no argument is specified, use default value: NOT_NULL
        ?: return NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)

    return when (enumValue.asString()) {
        "ALWAYS" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)
        "MAYBE", "NEVER" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)
        "UNKNOWN" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.FORCE_FLEXIBILITY)
        else -> null
    }
}
