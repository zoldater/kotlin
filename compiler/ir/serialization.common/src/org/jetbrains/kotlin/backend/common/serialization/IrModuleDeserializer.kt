/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IrExtensionGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun IrSymbol.kind(): BinarySymbolData.SymbolKind {
    return when (this) {
        is IrClassSymbol -> BinarySymbolData.SymbolKind.CLASS_SYMBOL
        is IrConstructorSymbol -> BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL
        is IrSimpleFunctionSymbol -> BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
        is IrPropertySymbol -> BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
        is IrEnumEntrySymbol -> BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL
        is IrTypeAliasSymbol -> BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL
        else -> error("Unexpected symbol kind $this")
    }
}

abstract class IrModuleDeserializer(val moduleDescriptor: ModuleDescriptor) {
    abstract operator fun contains(idSig: IdSignature): Boolean
    abstract fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol

    open fun declareIrSymbol(symbol: IrSymbol) {
        assert(symbol.isPublicApi)
        assert(symbol.descriptor !is WrappedDeclarationDescriptor<*>)
        deserializeIrSymbol(symbol.signature, symbol.kind())
    }

    open val klib: IrLibrary get() = error("Unsupported operation")

    open fun init() = init(this)

    open fun init(delegate: IrModuleDeserializer) {}

    open fun addModuleReachableTopLevel(idSig: IdSignature) { error("Unsupported Operation (sig: $idSig") }

    open fun deserializeReachableDeclarations() { error("Unsupported Operation") }

    open fun postProcess() {}

    abstract val moduleFragment: IrModuleFragment

    abstract val moduleDependencies: Collection<IrModuleDeserializer>

    open val strategy: DeserializationStrategy = DeserializationStrategy.ONLY_DECLARATION_HEADERS
}

// Used to resolve built in symbols like `kotlin.ir.internal.*` or `kotlin.FunctionN`
class IrModuleDeserializerWithBuiltIns(
    private val builtIns: IrBuiltIns,
    private val functionFactory: IrAbstractFunctionFactory,
    private val delegate: IrModuleDeserializer
) : IrModuleDeserializer(delegate.moduleDescriptor) {

    init {
        // TODO: figure out how it should work for K/N
//        assert(builtIns.builtIns.builtInsModule === delegate.moduleDescriptor)
    }

    private val irBuiltInsMap = builtIns.knownBuiltins.map {
        val symbol = (it as IrSymbolOwner).symbol
        symbol.signature to symbol
    }.toMap()

    private fun checkIsFunctionInterface(idSig: IdSignature): Boolean {
        val publicSig = idSig.asPublic() ?: return false

        if (publicSig.packageFqn !in functionalPackages) return false

        val declarationFqn = publicSig.declarationFqn

        if (declarationFqn.isRoot) return false

        val fqnParts = declarationFqn.pathSegments()

        val className = fqnParts.first()

        return functionPattern.matcher(className.asString()).find()
    }

    override operator fun contains(idSig: IdSignature): Boolean {
        if (idSig in irBuiltInsMap) return true

        return checkIsFunctionInterface(idSig) || idSig in delegate
    }

    override fun deserializeReachableDeclarations() {
        delegate.deserializeReachableDeclarations()
    }

    private fun computeFunctionDescriptor(className: Name): FunctionClassDescriptor {
        val nameString = className.asString()
        val isK = nameString[0] == 'K'
        val isSuspend = (if (isK) nameString[1] else nameString[0]) == 'S'
        val arity = nameString.run { substring(indexOfFirst { it.isDigit() }).toInt(10) }
        return functionFactory.run {
            when {
                isK && isSuspend -> kSuspendFunctionClassDescriptor(arity)
                isK -> kFunctionClassDescriptor(arity)
                isSuspend -> suspendFunctionClassDescriptor(arity)
                else -> functionClassDescriptor(arity)
            }
        }
    }

    private fun resolveFunctionalInterface(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        val publicSig = idSig.asPublic() ?: error("$idSig has to be public")

        val fqnParts = publicSig.declarationFqn.pathSegments()
        val className = fqnParts.firstOrNull() ?: error("Expected class name for $idSig")

        val functionDescriptor = computeFunctionDescriptor(className)
        val topLevelSignature = IdSignature.PublicSignature(publicSig.packageFqn, FqName(className.asString()), null, publicSig.mask)

        val functionClass = when (functionDescriptor.functionKind) {
            FunctionClassDescriptor.Kind.KSuspendFunction -> functionFactory.kSuspendFunctionN(functionDescriptor.arity) { callback ->
                declareClassFromLinker(functionDescriptor, topLevelSignature) { callback(it) }
            }
            FunctionClassDescriptor.Kind.KFunction -> functionFactory.kFunctionN(functionDescriptor.arity) { callback ->
                declareClassFromLinker(functionDescriptor, topLevelSignature) { callback(it) }
            }
            FunctionClassDescriptor.Kind.SuspendFunction -> functionFactory.suspendFunctionN(functionDescriptor.arity) { callback ->
                declareClassFromLinker(functionDescriptor, topLevelSignature) { callback(it) }
            }
            FunctionClassDescriptor.Kind.Function -> functionFactory.functionN(functionDescriptor.arity) { callback ->
                declareClassFromLinker(functionDescriptor, topLevelSignature) { callback(it) }
            }
        }

        return when (fqnParts.size) {
            1 -> functionClass.symbol.also { assert(symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) }
            2 -> {
                val memberName = fqnParts[1]!!
                functionClass.declarations.single { it is IrDeclarationWithName && it.name == memberName }.let {
                    (it as IrSymbolOwner).symbol
                }
            }
            3 -> {
                assert(idSig is IdSignature.AccessorSignature)
                assert(symbolKind == BinarySymbolData.SymbolKind.FUNCTION_SYMBOL)
                val propertyName = fqnParts[1]!!
                val accessorName = fqnParts[2]!!
                functionClass.declarations.filterIsInstance<IrProperty>().single { it.name == propertyName }.let { p ->
                    p.getter?.let { g -> if (g.name == accessorName) return g.symbol }
                    p.setter?.let { s -> if (s.name == accessorName) return s.symbol }
                    error("No accessor found for signature $idSig")
                }
            }
            else -> error("No member found for signature $idSig")
        }
    }

    override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        irBuiltInsMap[idSig]?.let { return it }

        if (checkIsFunctionInterface(idSig)) return resolveFunctionalInterface(idSig, symbolKind)

        return delegate.deserializeIrSymbol(idSig, symbolKind)
    }

    override fun postProcess() {
        delegate.postProcess()
    }

    override fun init() {
        delegate.init(this)
    }

    override val klib: IrLibrary
        get() = delegate.klib

    override val strategy: DeserializationStrategy
        get() = delegate.strategy

    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        delegate.addModuleReachableTopLevel(idSig)
    }

    override val moduleFragment: IrModuleFragment get() = delegate.moduleFragment
    override val moduleDependencies: Collection<IrModuleDeserializer> get() = delegate.moduleDependencies
}

open class CurrentModuleDeserializer(
    override val moduleFragment: IrModuleFragment,
    override val moduleDependencies: Collection<IrModuleDeserializer>,
    private val symbolTable: SymbolTable,
    private val extensions: Collection<IrExtensionGenerator>
) : IrModuleDeserializer(moduleFragment.descriptor) {
    override fun contains(idSig: IdSignature): Boolean = false // TODO:

    override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        error("Unreachable execution: there could not be back-links")
    }

    override fun declareIrSymbol(symbol: IrSymbol) {
        declareIrSymbolImpl(symbol)
    }

    private fun referenceParentDescriptor(descriptor: DeclarationDescriptor): IrSymbol {
        return when (descriptor) {
            is ClassDescriptor -> symbolTable.referenceClass(descriptor)
            is PropertyDescriptor -> symbolTable.referenceProperty(descriptor)
            is PackageFragmentDescriptor -> moduleFragment.files.single { it.symbol.descriptor === descriptor }.symbol
            else -> error("Unexpected declaration parent $descriptor")
        }
    }

    private fun declareIrDeclaration(symbol: IrSymbol): IrDeclaration {
        for (extension in extensions) {
            extension.declare(symbol)?.let { return it }
        }

        return declareIrDeclarationDefault(symbol)
    }

    private fun declareIrDeclarationDefault(symbol: IrSymbol): IrDeclaration {
        return when (symbol) {
            is IrClassSymbol -> symbolTable.declareClass(offset, offset, IrDeclarationOrigin.DEFINED, symbol.descriptor)
            is IrConstructorSymbol -> symbolTable.declareConstructor(offset, offset, IrDeclarationOrigin.DEFINED, symbol.descriptor)
            is IrSimpleFunctionSymbol -> symbolTable.declareSimpleFunction(offset, offset, IrDeclarationOrigin.DEFINED, symbol.descriptor)
            is IrPropertySymbol -> symbolTable.declareProperty(offset, offset, IrDeclarationOrigin.DEFINED, symbol.descriptor)
            is IrTypeAliasSymbol -> TODO("Implement type alias $symbol")
            is IrEnumEntrySymbol -> symbolTable.declareEnumEntry(offset, offset, IrDeclarationOrigin.DEFINED, symbol.descriptor)
            else -> error("Unexpected symbol $symbol")
        }
    }

    private fun declareIrSymbolImpl(symbol: IrSymbol): IrSymbolOwner {
        if (symbol.isBound) return symbol.owner
        val descriptor = symbol.descriptor

        assert(descriptor !is WrappedDeclarationDescriptor<*>)

        val accessor = descriptor as? PropertyAccessorDescriptor

        val parent = descriptor.containingDeclaration ?: error("Expect non-root declaration $descriptor")
        val parentDeclaration = declareIrSymbolImpl(referenceParentDescriptor(parent)) as IrDeclarationContainer

        val declaredDeclaration = declareIrDeclaration(symbol).also {
            it.parent = parentDeclaration
            if (accessor != null) {
                val property = accessor.correspondingProperty
                val irProperty = declareIrSymbolImpl(referenceParentDescriptor(property)) as IrProperty
                val irAccessor = it as IrSimpleFunction
                irAccessor.correspondingPropertySymbol = irProperty.symbol
                if (accessor === property.getter) irProperty.getter = irAccessor
                if (accessor === property.setter) irProperty.setter = irAccessor
            } else {
                parentDeclaration.declarations.add(it)
            }
        }

        return declaredDeclaration as IrSymbolOwner
    }

    companion object {
        private const val offset = UNDEFINED_OFFSET
    }
}