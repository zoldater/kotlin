// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !LANGUAGE: +NewInference

abstract class KtConstructor<T : KtConstructor<T>>

class KtPrimaryConstructor : KtConstructor<KtPrimaryConstructor>()

class KtSecondaryConstructor : KtConstructor<KtSecondaryConstructor>()

val primaryConstructor: KtPrimaryConstructor = TODO()

val secondaryConstructors: List<KtSecondaryConstructor> = TODO()

fun test() {
    val constructors = listOf(primaryConstructor) + secondaryConstructors
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<KtConstructor<*>>")!>constructors<!>
}

//    val x: KtElementImplStub<out (KotlinPlaceHolderStub<out (KtElementImplStub<out (KotlinPlaceHolderStub<*>..KotlinPlaceHolderStub<*>?)>..KtElementImplStub<out (org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub<*>..org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub<*>?)>?)>..org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub<out (org.jetbrains.kotlin.psi.KtElementImplStub<out (org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub<*>..org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub<*>?)>..org.jetbrains.kotlin.psi.KtElementImplStub<out (KotlinPlaceHolderStub<*>..KotlinPlaceHolderStub<*>?)>?)>?)> = TODO()