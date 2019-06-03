/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.names.FirClassId
import org.jetbrains.kotlin.fir.names.FirFqName
import org.jetbrains.kotlin.fir.names.FirName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

fun String.intern(session: FirSession): FirName {
    return session.nameFactory.create(this)
}

fun Name.intern(session: FirSession): FirName {
    return session.nameFactory.create(asString())
}

fun FqName.intern(session: FirSession): FirFqName {
    return FirFqName(this.stringPathSegments().map { it.intern(session) }.toTypedArray())
}

fun ClassId.intern(session: FirSession): FirClassId {
    return FirClassId(packageFqName.intern(session), relativeClassName.intern(session))
}