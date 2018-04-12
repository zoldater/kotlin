/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.builders

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.backend.FirBasedIrBuilderContext
import org.jetbrains.kotlin.fir.backend.descriptors.FirAnnotationDescriptor
import org.jetbrains.kotlin.fir.backend.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class IrModuleBuilder(val context: FirBasedIrBuilderContext) {

    private val annotationBuilder = IrAnnotationBuilder(context)

    fun generateModuleFragmentWithoutDependencies(files: Collection<FirFile>): IrModuleFragment =
        IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            irModule.files.addAll(generateFiles(files))
        }

    private fun generateFiles(files: Collection<FirFile>): List<IrFile> {

        return files.map { file ->
            generateSingleFile(file)
        }
    }

    private fun generateSingleFile(file: FirFile): IrFile {
        val irFile = createEmptyIrFile(file)
        val builder = IrDeclarationBuilder(context, irFile.packageFragmentDescriptor)

        for (annotation in file.annotations) {
            val annotationDescriptor = FirAnnotationDescriptor(annotation)
            irFile.fileAnnotations.add(annotationDescriptor)
            irFile.annotations.add(annotationBuilder.generateAnnotationConstructorCall(annotationDescriptor))
        }

        for (declaration in file.declarations) {
            irFile.declarations.add(builder.generateMemberDeclaration(declaration))
        }

        return irFile

    }

    private fun ModuleDescriptor.findPackageFragmentForFile(file: FirFile): PackageFragmentDescriptor =
        getPackage(file.packageFqName).fragments
            .filterIsInstance<LazyPackageDescriptor>()
            // TODO: get rid of PSI?
            .first { it.declarationProvider.containsFile(file.psi as KtFile) }


    private fun createEmptyIrFile(file: FirFile): IrFileImpl {
        val fileEntry = context.sourceManager.getOrCreateFileEntry(file)
        val hackedPackageFragmentDescriptor = FirPackageFragmentDescriptor(file.packageFqName, context.moduleDescriptor)

        val irFile = IrFileImpl(fileEntry, hackedPackageFragmentDescriptor)
        context.sourceManager.putFileEntry(irFile, fileEntry)
        return irFile
    }

}