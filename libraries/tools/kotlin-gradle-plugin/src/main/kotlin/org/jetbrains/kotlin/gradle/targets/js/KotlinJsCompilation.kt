 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectPackage
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

class KotlinJsCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinJsOptions>(target, name), KotlinCompilationWithResources<KotlinJsOptions> {
    var packageName = buildNpmProjectName()
        set(value) {
            NpmResolver.checkModification(target.project)
            field = value
        }

    private var packageJson: PackageJson? = null
    private var resolvedNpmPackage: NpmProjectPackage? = null

    val npmPackage: NpmProjectPackage
        get() = resolvedNpmPackage ?: error("This property accessible only after NpmResolve task execution")

    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    override val compileKotlinTask: Kotlin2JsCompile
        get() = super.compileKotlinTask as Kotlin2JsCompile

    private fun buildNpmProjectName(): String {
        val compilation = this
        val project = target.project
        val name = StringBuilder()

        name.append(project.rootProject.name)

        if (project != project.rootProject) {
            name.append("-")
            name.append(project.name)
        }

        if (target.name.isNotEmpty() && target.name.toLowerCase() != "js") {
            name.append("-").append(target.name)
        }

        if (compilation.name != KotlinCompilation.MAIN_COMPILATION_NAME) {
            name.append("-").append(compilation.name)
        }

        return name.toString()
    }
}