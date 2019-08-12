/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.SLRUMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible
import kotlin.script.experimental.api.valueOrNull

class ScriptsCompilationConfigurationCache(private val project: Project) {

    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                clearCaches()
            }
        })
    }

    private val cacheLock = ReentrantReadWriteLock()

    private val scriptDependenciesCache = SLRUCacheWithLock<ScriptCompilationConfigurationResult>()
    private val scriptsModificationStampsCache = SLRUCacheWithLock<Long>()

    operator fun get(file: KtFile): ScriptCompilationConfigurationResult? = scriptDependenciesCache.get(file)

    fun shouldRunDependenciesUpdate(file: KtFile): Boolean {
        return scriptsModificationStampsCache.replace(file, file.modificationStamp) != file.modificationStamp
    }

    private val scriptsDependenciesClasspathScopeCache: MutableMap<VirtualFile, GlobalSearchScope> = ConcurrentFactoryMap.createWeakMap {
        val ktFile = PsiManager.getInstance(project).findFile(it) as? KtFile ?: return@createWeakMap GlobalSearchScope.EMPTY_SCOPE

        val compilationConfiguration = scriptDependenciesCache.get(ktFile)?.valueOrNull()
            ?: return@createWeakMap GlobalSearchScope.EMPTY_SCOPE

        val roots = compilationConfiguration.dependenciesClassPath
        val sdk = javaHomeToSdk(compilationConfiguration.javaHome)

        @Suppress("FoldInitializerAndIfToElvis")
        if (sdk == null) {
            return@createWeakMap NonClasspathDirectoriesScope.compose(ScriptDependenciesManager.toVfsRoots(roots))
        }

        return@createWeakMap NonClasspathDirectoriesScope.compose(
            sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() +
                    ScriptDependenciesManager.toVfsRoots(roots)
        )
    }

    fun scriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope {
        return scriptsDependenciesClasspathScopeCache[file] ?: GlobalSearchScope.EMPTY_SCOPE
    }

    private val scriptsSdksCache: MutableMap<VirtualFile, Sdk> = ConcurrentFactoryMap.createWeakMap {
        val ktFile = PsiManager.getInstance(project).findFile(it) as? KtFile
            ?: return@createWeakMap ScriptDependenciesManager.getScriptDefaultSdk(project)

        return@createWeakMap scriptSdkOrDefault(scriptDependenciesCache.get(ktFile)?.valueOrNull()?.javaHome)
    }

    private fun scriptSdkOrDefault(javaHome: File?): Sdk? {
        return javaHomeToSdk(javaHome) ?: ScriptDependenciesManager.getScriptDefaultSdk(project)
    }

    fun scriptSdkOrDefault(file: VirtualFile): Sdk? {
        return scriptsSdksCache[file]
    }

    private fun javaHomeToSdk(javaHome: File?): Sdk? {
        // workaround for mismatched gradle wrapper and plugin version
        val javaHomeVF = try {
            javaHome?.let { VfsUtil.findFileByIoFile(it, true) }
        } catch (e: Throwable) {


            null
        } ?: return null

        return getAllProjectSdks().find { it.homeDirectory == javaHomeVF }
    }

    val allSdks by ClearableLazyValue(cacheLock) {
        scriptDependenciesCache.getAll()
            .mapNotNull { scriptSdkOrDefault(it.value.valueOrNull()?.javaHome) }
            .distinct()
    }

    private val allNonIndexedSdks by ClearableLazyValue(cacheLock) {
        scriptDependenciesCache.getAll()
            .mapNotNull { scriptSdkOrDefault(it.value.valueOrNull()?.javaHome) }
            .filterNonModuleSdk()
            .distinct()
    }

    val allDependenciesClassFiles by ClearableLazyValue(cacheLock) {
        val sdkFiles = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.CLASSES).toList() }

        val scriptDependenciesClasspath = scriptDependenciesCache.getAll()
            .flatMap { it.value.valueOrNull()?.dependenciesClassPath ?: emptyList() }.distinct()

        sdkFiles + ScriptDependenciesManager.toVfsRoots(scriptDependenciesClasspath)
    }

    val allDependenciesSources by ClearableLazyValue(cacheLock) {
        val sdkSources = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.SOURCES).toList() }

        val scriptDependenciesSources = scriptDependenciesCache.getAll()
            .flatMap { it.value.valueOrNull()?.dependenciesSources ?: emptyList() }.distinct()
        sdkSources + ScriptDependenciesManager.toVfsRoots(scriptDependenciesSources)
    }

    val allDependenciesClassFilesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    val allDependenciesSourcesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    private fun Collection<Sdk>.filterNonModuleSdk(): List<Sdk> {
        val moduleSdks = ModuleManager.getInstance(project).modules.map { ModuleRootManager.getInstance(it).sdk }
        return filterNot { moduleSdks.contains(it) }
    }

    private fun onChange(files: List<KtFile>) {
        clearCaches()
        updateHighlighting(files)
    }

    private fun clearCaches() {
        this::allSdks.clearValue()
        this::allNonIndexedSdks.clearValue()

        this::allDependenciesClassFiles.clearValue()
        this::allDependenciesClassFilesScope.clearValue()

        this::allDependenciesSources.clearValue()
        this::allDependenciesSourcesScope.clearValue()

        scriptsDependenciesClasspathScopeCache.clear()
        scriptsSdksCache.clear()

        val kotlinScriptDependenciesClassFinder =
            Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()
    }

    private fun updateHighlighting(files: List<KtFile>) {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        GlobalScope.launch(EDT(project)) {
            files.filter { it.isValid }.forEach {
                DaemonCodeAnalyzer.getInstance(project).restart(it)
            }
        }
    }

    fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean {
        val scriptSdk = javaHomeToSdk(compilationConfiguration.javaHome) ?: ScriptDependenciesManager.getScriptDefaultSdk(project)
        return (scriptSdk != null && !allSdks.contains(scriptSdk)) ||
                !allDependenciesClassFiles.containsAll(ScriptDependenciesManager.toVfsRoots(compilationConfiguration.dependenciesClassPath)) ||
                !allDependenciesSources.containsAll(ScriptDependenciesManager.toVfsRoots(compilationConfiguration.dependenciesSources))
    }

    fun clear() {
        val keys = scriptDependenciesCache.getAll().map { it.key }.toList()

        scriptDependenciesCache.clear()
        scriptsModificationStampsCache.clear()

        updateHighlighting(keys)
    }

    fun save(file: KtFile, new: ScriptCompilationConfigurationResult): Boolean {
        val old = scriptDependenciesCache.replace(file, new)
        val changed = new != old
        if (changed) {
            onChange(listOf(file))
        }

        return changed
    }

    fun delete(file: KtFile): Boolean {
        val changed = scriptDependenciesCache.remove(file)
        if (changed) {
            onChange(listOf(file))
        }
        return changed
    }
}

private fun <R> KProperty0<R>.clearValue() {
    isAccessible = true
    (getDelegate() as ClearableLazyValue<*, *>).clear()
}

private class ClearableLazyValue<in R, out T : Any>(
    private val lock: ReentrantReadWriteLock,
    private val compute: () -> T
) : ReadOnlyProperty<R, T> {
    override fun getValue(thisRef: R, property: KProperty<*>): T {
        lock.write {
            if (value == null) {
                value = compute()
            }
            return value!!
        }
    }

    private var value: T? = null


    fun clear() {
        lock.write {
            value = null
        }
    }
}

private class SLRUCacheWithLock<T> {
    private val lock = ReentrantReadWriteLock()

    val cache = SLRUMap<KtFile, T>(
        ScriptsCompilationConfigurationCache.MAX_SCRIPTS_CACHED,
        ScriptsCompilationConfigurationCache.MAX_SCRIPTS_CACHED
    )

    fun get(value: KtFile): T? = lock.write {
        cache[value]
    }

    fun getOrPut(key: KtFile, defaultValue: () -> T): T = lock.write {
        val value = cache.get(key)
        return if (value == null) {
            val answer = defaultValue()
            replace(key, answer)
            answer
        } else {
            value
        }
    }

    fun remove(file: KtFile) = lock.write {
        cache.remove(file)
    }

    fun getAll(): Collection<Map.Entry<KtFile, T>> = lock.write {
        cache.entrySet()
    }

    fun clear() = lock.write {
        cache.clear()
    }

    fun replace(file: KtFile, value: T): T? = lock.write {
        val old = get(file)
        cache.put(file, value)
        old
    }
}