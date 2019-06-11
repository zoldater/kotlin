/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs

/**
 * Visits given gradle [project] for all of its [NpmProject],
 * creates `package.json` for them ([NpmProjectPackage]) and runs
 * selected [NodeJsRootExtension.packageManager] to download and install all of it's dependencies.
 */
internal class NpmProjectVisitor(val resolver: NpmResolver, override val project: Project): NpmProjects {
    override val npmProjects = mutableListOf<NpmProjectPackage>()
    private val byCompilation = mutableMapOf<KotlinJsCompilation, NpmProjectPackage>()
    private val byNpmDependency = mutableMapOf<NpmDependency, NpmProjectPackage>()
    override val taskRequirements = mutableMapOf<RequiresNpmDependencies, Collection<RequiredKotlinJsDependency>>()
    private val requiredFromTasksByCompilation = mutableMapOf<KotlinJsCompilation, MutableList<RequiresNpmDependencies>>()

    override val npmProjectsByCompilation: Map<KotlinJsCompilation, NpmProjectPackage>
        get() = byCompilation

    override val npmProjectsByNpmDependency: Map<NpmDependency, NpmProjectPackage>
        get() = byNpmDependency

    private fun addTaskRequirements(task: RequiresNpmDependencies) {
        val requirements = task.requiredNpmDependencies.toList()

        taskRequirements[task] = requirements

        requiredFromTasksByCompilation
            .getOrPut(task.compilation) { mutableListOf() }
            .add(task)
    }

    private fun addNpmProject(resolved: NpmProjectPackage) {
        val name = resolved.npmProject.name
        val npmProjectsByName = resolver.npmProjectsByName
        if (name in npmProjectsByName) {
            error("NPM project name clash \"name\": ${resolved.npmProject.compilation} and ${npmProjectsByName[name]!!.npmProject.compilation}")
        }

        npmProjectsByName[name] = resolved

        val compilation = resolved.npmProject.compilation
        byCompilation[compilation] = resolved
        resolved.npmDependencies.forEach {
            byNpmDependency[it] = resolved
        }
        npmProjects.add(resolved)
    }

    fun visitProject(): NpmProjects {
        visitTasksRequiredDependencies()

        val kotlin = project.kotlinExtensionOrNull

        if (kotlin != null) {
            when (kotlin) {
                is KotlinSingleTargetExtension -> visitTarget(kotlin.target)
                is KotlinMultiplatformExtension -> kotlin.targets.forEach {
                    visitTarget(it)
                }
            }
        }

        return this
    }

    private fun visitTasksRequiredDependencies() {
        project.tasks.toList().forEach { task ->
            if (task.enabled && task is RequiresNpmDependencies) {
                addTaskRequirements(task)
            }
        }
    }

    private fun visitTarget(target: KotlinTarget) {
        if (target.platformType == KotlinPlatformType.js) {
            // note: compilation may be KotlinWithJavaTarget for old Kotlin2JsPlugin
            // visit main compliation first, as other compilation of this project may depend on it
            // (other cases currently is not supported by org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver.findDependentResolvedNpmProject)
            target.compilations.toList()
                .filterIsInstance<KotlinJsCompilation>()
                .sortedBy { if (it.compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) 0 else 1 }
                .forEach {
                    visitCompilation(it)
                }
        }
    }

    private fun visitCompilation(compilation: KotlinJsCompilation) {
        val project = compilation.target.project
        val npmProject = compilation.npmProject
        val name = npmProject.name
        val packageJson = PackageJson(
            name,
            fixSemver(project.version.toString())
        )
        val npmDependencies = mutableSetOf<NpmDependency>()
        val gradleDeps = NpmGradleDependencies()

        val aggregateConfiguration = project.configurations.create("$name-npm-${project.configurations.size}") {
            it.usesPlatformOf(compilation.target)
            it.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(compilation.target))
            it.isVisible = false
            it.isCanBeConsumed = false
            it.isCanBeResolved = true
            it.description = "NPM configuration for $compilation."
        }

        compilation.allKotlinSourceSets.forEach { sourceSet ->
            sourceSet.relatedConfigurationNames.forEach { configurationName ->
                val configuration = project.configurations.getByName(configurationName)
                aggregateConfiguration.extendsFrom(configuration)
            }
        }

        packageJson.main = npmProject.main

        val requiredByTasks = requiredFromTasksByCompilation[compilation]
        var nodeModulesRequired = false
        if (requiredByTasks != null && requiredByTasks.isNotEmpty()) {
            val toolsConfiguration = project.configurations.create("$name-jsTools-${project.configurations.size}")
            requiredByTasks.forEach {
                if (it.nodeModulesRequired) nodeModulesRequired = true
                it.requiredNpmDependencies.forEach { requirement ->
                    toolsConfiguration.dependencies.add(requirement.createDependency(project))
                }
            }
            aggregateConfiguration.extendsFrom(toolsConfiguration)
        }

        visitConfiguration(aggregateConfiguration, npmDependencies, gradleDeps)

        npmDependencies.forEach {
            packageJson.dependencies[it.key] = resolver.chooseVersion(packageJson.dependencies[it.key], it.version)
        }

        gradleDeps.externalModules.forEach {
            packageJson.dependencies[it.name] = it.version
        }

        gradleDeps.internalModules.forEach { target ->
            val resolvedTarget = resolver.findDependentResolvedNpmProject(project, target)
            if (resolvedTarget != null) {
                packageJson.dependencies[resolvedTarget.packageJson.name] = resolvedTarget.packageJson.version
            }
        }

        project.nodeJs.packageJsonHandlers.forEach {
            it(packageJson)
        }

        val npmPackage = NpmProjectPackage(project, npmProject, npmDependencies, gradleDeps, packageJson, nodeModulesRequired)
        npmPackage.packageJson.saveTo(npmProject.packageJsonFile)

        resolver.packageManager.resolveProject(npmPackage)

        addNpmProject(npmPackage)
    }

    private fun visitConfiguration(
        configuration: Configuration,
        npmDependencies: MutableSet<NpmDependency>,
        gradleDependencies: NpmGradleDependencies
    ) {
        resolver.gradleNodeModules.collectDependenciesFromConfiguration(configuration, gradleDependencies)

        configuration.allDependencies.forEach { dependency ->
            when (dependency) {
                is NpmDependency -> npmDependencies.add(dependency)
            }
        }
    }
}