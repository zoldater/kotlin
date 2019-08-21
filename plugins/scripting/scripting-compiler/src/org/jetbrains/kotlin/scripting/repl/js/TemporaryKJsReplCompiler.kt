/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsMangler
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.StringScriptSource

class TemporaryKlibCompiler(
    val environment: KotlinCoreEnvironment,
    val namer: NameTables = NameTables(emptyList())
) {
    val analyzerEngine = JsReplCodeAnalyzer(environment)
    val symbolTable = SymbolTable()
    val deserializer: JsIrLinker
    val context: JsIrBackendContext
    val irBuiltIns: IrBuiltIns
    val deserializedModuleFragments: List<IrModuleFragment>

    val stdlibCompiledResult: String

    init {
        val messageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY] as MessageCollector

        setIdeaIoUseFallback()

        val snippet = ""
        val lineNumber = 0
        val codeLine = makeReplCodeLine(lineNumber, snippet)
        val sourceCode = StringScriptSource(snippet, "line-$lineNumber.kts")

        val snippetKtFile = getScriptKtFile(
            sourceCode,
            snippet,
            environment.project
        ).valueOrNull()
        require(snippetKtFile != null)

        analyzerEngine.analyzeReplLine(snippetKtFile, codeLine).also {
            AnalyzerWithCompilerReport.reportDiagnostics(it, messageCollector)
            require(!messageCollector.hasErrors())
        }

        val psi2ir = Psi2IrTranslator(environment.configuration.languageVersionSettings)
        val psi2irContext = psi2ir.createGeneratorContext(
            analyzerEngine.context.module,
            analyzerEngine.trace.bindingContext,
            symbolTable
        )

        irBuiltIns = psi2irContext.irBuiltIns
        deserializer = JsIrLinker(
            psi2irContext.moduleDescriptor,
            JsMangler,
            emptyLoggingContext,
            irBuiltIns,
            symbolTable
        ).also { it.isReplInitializing = true }

        deserializedModuleFragments = analyzerEngine.dependencies.map {
            deserializer.deserializeIrModuleHeader(analyzerEngine.modulesStructure.getModuleDescriptor(it))!!
        }

        val irModuleFragment = psi2irContext.generateModuleFragment(listOf(snippetKtFile), deserializer)

        environment.configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        context = JsIrBackendContext(
            irModuleFragment.descriptor,
            psi2irContext.irBuiltIns,
            psi2irContext.symbolTable,
            irModuleFragment,
            emptySet(),
            environment.configuration,
            true
        )

        val irFiles = sortDependencies(deserializedModuleFragments).flatMap { it.files } + irModuleFragment.files

        irModuleFragment.files.clear()
        if (namer.globalNames.names.isEmpty()) irModuleFragment.files += irFiles

        ExternalDependenciesGenerator(
            moduleDescriptor = irModuleFragment.descriptor,
            symbolTable = psi2irContext.symbolTable,
            irBuiltIns = psi2irContext.irBuiltIns
        ).generateUnboundSymbolsAsDependencies()

        stdlibCompiledResult = compileForRepl(
            context,
            null,
            irModuleFragment,
            namer
        )

        deserializer.isReplInitializing = false
    }
}

open class TemporaryKJsReplCompiler(
    private val environment: KotlinCoreEnvironment,
    private val namer: NameTables = NameTables(emptyList()),
    private val deserializer: JsIrLinker?,
    private var context: JsIrBackendContext?,
    private val analyzerEngine: JsReplCodeAnalyzer = JsReplCodeAnalyzer(environment),
    private val symbolTable: SymbolTable = SymbolTable()
) : ReplCompiler {
    constructor(environment: KotlinCoreEnvironment, namer: NameTables) : this(
        environment = environment,
        namer = namer,
        deserializer = null,
        context = null
    )

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> {
        return JsState(lock)
    }

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult {
        return ReplCheckResult.Ok()
    }

    //TODO: extract common
    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult {
        val snippet = codeLine.code
        val snippetId = codeLine.no

        val messageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY] as MessageCollector

        setIdeaIoUseFallback()

        val sourceCode = StringScriptSource(snippet, "line-$snippetId.kts")
        val snippetKtFile = getScriptKtFile(
            sourceCode,
            snippet,
            environment.project
        ).valueOr { return ReplCompileResult.Error(it.reports.joinToString { r -> r.message }) }

        analyzerEngine.analyzeReplLine(snippetKtFile, codeLine).also {
            AnalyzerWithCompilerReport.reportDiagnostics(it, messageCollector)
            if (messageCollector.hasErrors()) return ReplCompileResult.Error("Error while analysis")
        }

        val psi2ir = Psi2IrTranslator(environment.configuration.languageVersionSettings)
        val psi2irContext = psi2ir.createGeneratorContext(
            analyzerEngine.context.module,
            analyzerEngine.trace.bindingContext,
            symbolTable
        )

        val irModuleFragment = psi2irContext.generateModuleFragment(listOf(snippetKtFile), deserializer)

        if (context == null) {
            context = JsIrBackendContext(
                irModuleFragment.descriptor,
                psi2irContext.irBuiltIns,
                psi2irContext.symbolTable,
                irModuleFragment,
                emptySet(),
                environment.configuration,
                true
            )

            ExternalDependenciesGenerator(
                irModuleFragment.descriptor,
                psi2irContext.symbolTable,
                psi2irContext.irBuiltIns
            ).generateUnboundSymbolsAsDependencies()
        }

        with(context!!.implicitDeclarationFile) {
            if (!irModuleFragment.files.contains(this)) {
                irModuleFragment.files += this
            }
        }

        context!!.implicitDeclarationFile.declarations.clear()

        environment.configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        val code = compileForRepl(
            context!!,
            JsMainFunctionDetector.getMainFunctionOrNull(irModuleFragment),
            irModuleFragment,
            namer
        )

        return createCompileResult(
            LineId(codeLine),
            code
        )
    }
}
