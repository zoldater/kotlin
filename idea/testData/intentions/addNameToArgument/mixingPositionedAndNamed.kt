// COMPILER_ARGUMENTS: -XXLanguage:+NewInference -XXLanguage:+AllowMixedNamedArgumentWithPosition
// WITH_RUNTIME

fun foo(s: String, b: Boolean){}

fun bar() {
    foo(<caret>"", true)
}
