/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import org.jetbrains.plugins.gradle.model.ProjectImportAction

abstract class AbstractModelBuilderTestBase {
    fun addExtraProjectModelClasses(projectImportAction: ProjectImportAction, classes: Set<Class<*>>) {
        projectImportAction.addExtraProjectModelClasses(classes)
    }
}