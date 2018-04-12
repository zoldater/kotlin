/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.fir.backend.IrBuilder
import org.jetbrains.kotlin.fir.backend.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.ir.AbstractIrTextTestCase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractFir2IrTextTest : AbstractIrTextTestCase() {


    fun createSession(sourceScope: GlobalSearchScope, project: Project): FirSession {
        val moduleInfo = FirTestModuleInfo()
        val provider = FirProjectSessionProvider(project)
        return FirJavaModuleBasedSession(moduleInfo, provider, sourceScope).also {
            createSessionForDependencies(provider, moduleInfo, sourceScope)
        }
    }

    private fun createSessionForDependencies(
        provider: FirProjectSessionProvider, moduleInfo: FirTestModuleInfo, sourceScope: GlobalSearchScope
    ) {
        val dependenciesInfo = FirTestModuleInfo()
        moduleInfo.dependencies.add(dependenciesInfo)
        FirLibrarySession(dependenciesInfo, provider, GlobalSearchScope.notScope(sourceScope))
    }

    override fun generateIrModule(ignoreErrors: Boolean, shouldGenerate: (KtFile) -> Boolean): IrModuleFragment {
        val psiFiles = myFiles.psiFiles

        val project = psiFiles.first().project

        val scope = GlobalSearchScope.filesScope(project, psiFiles.map { it.virtualFile })
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
        val session = createSession(scope, project)

        val builder = RawFirBuilder(session)

        val transformer = FirTotalResolveTransformer()
        val firFiles = psiFiles.map {
            val firFile = builder.buildFirFile(it)
            (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)
            firFile
        }.also {
            try {
                transformer.processFiles(it)
            } catch (e: Exception) {
                throw e
            }
        }

        return IrBuilder().generateModule(FirModuleDescriptor(session), firFiles)
    }
}