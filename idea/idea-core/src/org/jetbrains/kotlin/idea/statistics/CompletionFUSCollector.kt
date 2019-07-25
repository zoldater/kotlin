/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

object CompletionFUSCollector {

    /**
     * @param time the number of milliseconds that was required to fill the maximum visible area of the completion popup
     * @param choicePosition needs to measure sorting. The number of finally chosen line in the completion popup. Send 0
     *                       if user don't choose any line.
     */
    fun log(time: String, choicePosition: Int, fileType: FileType) {
        val data: Map<String, String> = mutableMapOf()
        data.plus(Pair("window_population_time", time))
            .plus(Pair("file_type", fileType.toString()))
            .plus(Pair("choice_at_position", choicePosition))
        KotlinFUSLogger.log(FUSEventGroups.Editor, "Completion", data)
    }
}

enum class FileType {
    KT, GRADLEKTS, KTS
}