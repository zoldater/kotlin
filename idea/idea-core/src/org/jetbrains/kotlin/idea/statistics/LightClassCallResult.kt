/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

enum class LightClassCallResult {
    Success,
    UnimplementedLocalClass,
    UnimplementedKotlinCollection,
    JetBrainsPluginsOnly,
    ExternalPlugins,
    Disabled,
    RequestedForScript,
    Unknown
}

fun logLightClassCallStatistics(result: LightClassCallResult) {
    val data = mapOf("result" to result.toString())
    KotlinFUSLogger.log(FUSEventGroups.Editor, "LightClassCall", data)
}
