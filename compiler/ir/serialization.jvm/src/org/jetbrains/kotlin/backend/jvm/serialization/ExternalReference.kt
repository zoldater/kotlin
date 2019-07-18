/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


fun collectExternalReferences(toplevel: IrDeclarationContainer): List<IrDeclaration> {
    val collection = ExternalReferenceCollection(toplevel)
    toplevel.declarations.forEach {
        it.accept(
            object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
                    // Copy is stored in collection.references.
                    collection.getCopy(expression.symbol.owner)
                    super.visitFunctionAccess(expression)
                }

                override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?) {
                    // Copying a getter or setter will store the whole property in collection.references.
                    collection.getCopy((expression.getter ?: expression.setter ?: expression.field)!!.owner)
                    super.visitPropertyReference(expression, data)
                }
            },
            null
        )
    }
    return collection.getToplevels()
}

class ExternalReferenceCollection(
    val toplevel: IrDeclarationParent
) {
    val references =  mutableMapOf<IrSymbol, IrSymbolOwner>()

    fun getToplevels(): List<IrDeclaration> =
        references.mapNotNull {
            it.safeAs<IrDeclaration>()?.takeIf { it.isTopLevel }
        }

    // Make a copy, preserving only the information needed to reference the object from a different serialization unit.
    fun <T: IrSymbolOwner> getCopy(symbolOwner: T): T {

        if (symbolOwner == toplevel) return symbolOwner
        symbolOwner.safeAs<IrDeclaration>()?.findTopLevelDeclaration()?.let {
            // we are either in a toplevel class or in a toplevel function or property.
            if (it == toplevel || it.parent == toplevel) return symbolOwner
        }
        references[symbolOwner.symbol]?.let { return it as T }

        when {
            symbolOwner is IrPackageFragment ->
                return IrExternalPackageFragmentImpl(
                    DescriptorlessExternalPackageFragmentSymbol(),
                    symbolOwner.fqName
                ).apply {
                    symbol.bind(this)
                    references[symbolOwner.symbol] = this
                } as T

            symbolOwner is IrDeclaration -> {
                val parentCopy = getCopy(symbolOwner.parent as IrSymbolOwner) as IrDeclarationContainer
                val newDeclaration = symbolOwner.bodilessCopyTo(parentCopy).apply {
                    require(this is IrSymbolOwner)
                    references[symbolOwner.symbol] = this

                    // Special case: we need to handle relations between properties and their accessors and fields.
                    // This should be done after putting the created declaration into `references`.
                    when (symbolOwner) {
                        is IrProperty -> {
                            require(this is IrProperty)
                            getter = symbolOwner.getter?.bodilessCopyTo(parentCopy) as IrSimpleFunction?
                            setter = symbolOwner.setter?.bodilessCopyTo(parentCopy) as IrSimpleFunction?
                            backingField = symbolOwner.backingField?.bodilessCopyTo(parentCopy) as IrField?
                            parentCopy.declarations.add(this)
                        }
                        is IrSimpleFunction -> {
                            require(this is IrSimpleFunction)

                            // Here and in the next clause, we use the fact that _in unlowered IR_
                            // property accessors and fields are not in their parent's declaration list.
                            if (symbolOwner.correspondingPropertySymbol != null) {
                                correspondingPropertySymbol = symbolOwner.correspondingPropertySymbol!!.owner.bodilessCopyTo(parentCopy)
                                    .safeAs<IrProperty>()!!.symbol
                            } else {
                                parentCopy.declarations.add(this)
                            }

                        }
                        is IrField -> {
                            require(this is IrField)
                            if (symbolOwner.correspondingPropertySymbol != null) {
                                correspondingPropertySymbol = symbolOwner.correspondingPropertySymbol!!.owner.bodilessCopyTo(parentCopy)
                                    .safeAs<IrProperty>()!!.symbol
                            } else {
                                parentCopy.declarations.add(this)
                            }
                        }
                        else -> parentCopy.declarations.add(this)
                    }
                }
                /* Kludge to work around annotations that are not correctly generated by Psi2Ir */
                val annotations = symbolOwner.annotations.filter { annotation ->
                    (0 until annotation.valueArgumentsCount).all { i ->
                        annotation.getValueArgument(i)?.type !is IrErrorType
                    }
                }
                annotations.mapTo(newDeclaration.annotations) {
                    it.deepCopyWithExternalReferences(this)
                }
                return newDeclaration as T
            }
            else -> error("should never be reached")
        }
    }

    // Type parameters in return/field types are not remapped, but this should not matter for serialization.
    private fun IrDeclaration.bodilessCopyTo(newParent: IrDeclarationParent): IrDeclaration = when (this) {
        is IrEnumEntry -> run {
            val descriptor = WrappedEnumEntryDescriptor()
            IrEnumEntryImpl(
                startOffset, endOffset, origin, IrEnumEntrySymbolImpl(descriptor), name
            ).apply {
                descriptor.bind(this)
                parent = newParent
            }
        }
        is IrClass -> {
            val descriptor = WrappedClassDescriptor()
            IrClassImpl(
                startOffset, endOffset, origin,
                IrClassSymbolImpl(descriptor),
                name, kind, visibility, modality, isCompanion, isInner, isData, isExternal, isInline
            ).apply {
                descriptor.bind(this)
                parent = newParent
                copyTypeParametersFrom(this@bodilessCopyTo)
            }
        }
        is IrConstructor -> {
            val descriptor = WrappedClassConstructorDescriptor()
            IrConstructorImpl(
                startOffset, endOffset, origin,
                IrConstructorSymbolImpl(descriptor),
                name, visibility, returnType, isInline, isExternal, isPrimary
            ).apply {
                descriptor.bind(this)
                parent = newParent
                copyParameterDeclarationsFrom(this@bodilessCopyTo)
            }
        }
        is IrSimpleFunction -> {
            val descriptor = WrappedSimpleFunctionDescriptor()
            IrFunctionImpl(
                startOffset, endOffset, origin,
                IrSimpleFunctionSymbolImpl(descriptor),
                name, visibility, modality, returnType, isInline, isExternal, isTailrec, isSuspend
            ).apply {
                descriptor.bind(this)
                parent = newParent
                copyParameterDeclarationsFrom(this@bodilessCopyTo)
            }
        }
        is IrProperty -> {
            val descriptor = WrappedPropertyDescriptor()
            IrPropertyImpl(
                startOffset, endOffset, origin,
                IrPropertySymbolImpl(descriptor),
                name, visibility, modality, isVar, isConst, isLateinit, isDelegated, isExternal
            ).apply {
                descriptor.bind(this)
                parent = newParent
            }
        }
        is IrField -> {
            val descriptor = WrappedFieldDescriptor()
            IrFieldImpl(
                startOffset, endOffset, origin,
                IrFieldSymbolImpl(descriptor),
                name, type, visibility, isFinal, isExternal, isStatic
            ).apply {
                descriptor.bind(this)
                parent = newParent
            }
        }
        else -> error("Unsupported declaration type $this")
    }
}

// This symbol remapper is only applied to annotations, so many types of entities should never occur.

private class ExternalReferenceSymbolRemapper(val referenceCollection: ExternalReferenceCollection) : SymbolRemapper {
    override fun getDeclaredClass(symbol: IrClassSymbol) = error("should never be called")
    override fun getDeclaredFunction(symbol: IrSimpleFunctionSymbol) = error("should never be called")
    override fun getDeclaredProperty(symbol: IrPropertySymbol) = error("should never be called")
    override fun getDeclaredField(symbol: IrFieldSymbol) = error("should never be called")
    override fun getDeclaredFile(symbol: IrFileSymbol) = error("should never be called")
    override fun getDeclaredConstructor(symbol: IrConstructorSymbol) = error("should never be called")
    override fun getDeclaredEnumEntry(symbol: IrEnumEntrySymbol) = error("should never be called")
    override fun getDeclaredExternalPackageFragment(symbol: IrExternalPackageFragmentSymbol) = error("should never be called")
    override fun getDeclaredVariable(symbol: IrVariableSymbol) = error("should never be called")
    override fun getDeclaredLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol) = error("should never be called")
    override fun getDeclaredTypeParameter(symbol: IrTypeParameterSymbol) = error("should never be called")
    override fun getDeclaredValueParameter(symbol: IrValueParameterSymbol) = error("should never be called")
    override fun getReferencedClass(symbol: IrClassSymbol) = referenceCollection.getCopy(symbol.owner).symbol
    override fun getReferencedClassOrNull(symbol: IrClassSymbol?) = symbol?.let { getReferencedClass(it) }
    override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol) = referenceCollection.getCopy(symbol.owner).symbol
    override fun getReferencedVariable(symbol: IrVariableSymbol) = error("should never be called")
    override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol) = error("should never be called")
    override fun getReferencedField(symbol: IrFieldSymbol) = referenceCollection.getCopy(symbol.owner).symbol
    override fun getReferencedConstructor(symbol: IrConstructorSymbol) = referenceCollection.getCopy(symbol.owner).symbol
    override fun getReferencedValue(symbol: IrValueSymbol) = referenceCollection.getCopy(symbol.owner).symbol
    override fun getReferencedFunction(symbol: IrFunctionSymbol) = referenceCollection.getCopy(symbol.owner).symbol
    override fun getReferencedProperty(symbol: IrPropertySymbol) = referenceCollection.getCopy(symbol.owner).symbol
    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol) = referenceCollection.getCopy(symbol.owner).symbol
    override fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol) = error("should never be called")
    override fun getReferencedClassifier(symbol: IrClassifierSymbol) = referenceCollection.getCopy(symbol.owner).symbol  as IrClassifierSymbol
}

// Copied with modifications from `deepCopyWithSymbols`
private fun <T : IrElement> T.deepCopyWithExternalReferences(
    referenceCollection: ExternalReferenceCollection,
    initialParent: IrDeclarationParent? = null
): T {
    val symbolRemapper = ExternalReferenceSymbolRemapper(referenceCollection)
    val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
    return transform(DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper), null).patchDeclarationParents(initialParent) as T
}

// Copied from MoveBodilessDeclarationsToSeparatePlace.kt

private class DescriptorlessExternalPackageFragmentSymbol : IrExternalPackageFragmentSymbol {
    override val descriptor: PackageFragmentDescriptor
        get() = error("Operation is unsupported")

    private var _owner: IrExternalPackageFragment? = null
    override val owner get() = _owner!!

    override val isBound get() = _owner != null

    override fun bind(owner: IrExternalPackageFragment) {
        _owner = owner
    }
}

