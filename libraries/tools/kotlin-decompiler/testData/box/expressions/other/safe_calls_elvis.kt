fun foo(x: Int?): String = x?.toString() ?: "OK"

fun box() = foo(null)