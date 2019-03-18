// !LANGUAGE: -NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !CHECK_TYPE

//fun <K> select(x: K, y: K): K = x

fun bar(x: Any) {}

fun foo(y: Double) {
    bar(123)
}

