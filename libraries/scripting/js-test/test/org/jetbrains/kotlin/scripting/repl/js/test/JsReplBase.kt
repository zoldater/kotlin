/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.platform
import org.jetbrains.kotlin.scripting.repl.js.*
import java.io.Closeable
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.jvm.JsDependency

class JsReplBase : Closeable {
    val compiler: TemporaryKJsReplCompiler
    val jsEngine: JsReplEvaluator

    private var snippetId: Int = 1 //index 0 for klib
    fun newSnippetId(): Int = snippetId++

    init {
        compiler = TemporaryKJsReplCompiler(environment, loadNames())

        jsEngine = JsReplEvaluator()
        jsEngine.eval(
            jsEngine.createState(),
            createCompileResult(loadStdlibCompiledResult())
        )
    }

    override fun close() {
//        Disposer.dispose(disposable) //TODO??////?/?//
    }

    companion object {
        private val disposable = Disposer.newDisposable()
        private val collector: ReplMessageCollector = ReplMessageCollector()
        private val configuration = loadConfiguration()

        val environment = KotlinCoreEnvironment.createForProduction(
            disposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES
        )
        val klibCompiler = TemporaryKlibCompiler(environment).also {
            saveNames(it.namer)
            saveStdlibCompiledResult(it.stdlibCompiledResult)
        }

        private fun loadConfiguration(): CompilerConfiguration {
            val configuration = CompilerConfiguration()
            configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, ScriptingCompilerConfigurationComponentRegistrar())
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, collector)
            configuration.put(CommonConfigurationKeys.MODULE_NAME, "repl.kts")
            val scriptConfiguration = ScriptCompilationConfiguration {
                baseClass("kotlin.Any")
                dependencies.append(JsDependency("compiler/ir/serialization.js/build/fullRuntime/klib"))
                platform.put("JS")
            }
            configuration.add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromConfigurations(ScriptingHostConfiguration(), scriptConfiguration, null)
            )
            return configuration
        }
    }
}

fun loadNames(): NameTables {
    val namer = NameTables(emptyList())

    Files.newBufferedReader(Paths.get("compiler/ir/serialization.js/build/fullRuntime/klib/mappedNames.txt")).use { reader ->
        var line: String? = reader.readLine()
        while (line != null) {
            val (key, value) = line.split(" ")
            namer.globalNames.mappedNames[key] = value
            namer.globalNames.reserved.add(value)
            line = reader.readLine()
        }
    }
    return namer
}

fun saveNames(namer: NameTables) {
    FileWriter("compiler/ir/serialization.js/build/fullRuntime/klib/mappedNames.txt").use { writer ->
        for (entry in namer.globalNames.mappedNames) {
            writer.write("${entry.key} ${entry.value}" + System.lineSeparator())
        }
    }
}

fun saveStdlibCompiledResult(stdlibCompiledResult: String) {
    FileWriter("compiler/ir/serialization.js/build/fullRuntime/klib/stdlibCompiledResult.js").use { writer ->
        writer.write(stdlibCompiledResult)
    }
}

fun loadStdlibCompiledResult(): String {
    val stdlibCompiledResult = StringBuilder()
    Files.newBufferedReader(Paths.get("compiler/ir/serialization.js/build/fullRuntime/klib/stdlibCompiledResult.js")).use { reader ->
        var line: String? = reader.readLine()
        while (line != null) {
            stdlibCompiledResult.append(line)
            line = reader.readLine()
        }
    }
    return stdlibCompiledResult.toString()
}
