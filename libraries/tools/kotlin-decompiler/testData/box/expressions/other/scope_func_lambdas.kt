fun foo(x: Int, y: Int, body: (Int, Int) -> String): String = body.invoke(x, y)

fun bar(x: Int?, body: (Int?) -> String?) = x?.let { body.invoke(it) }

fun box(): String {
    val lmb: (Int?) -> String? = { i -> i.toString() }
    val t = bar(null, lmb)
    val z = bar(42) { i -> "OK" }
    return z ?: foo(2, 4) { a, b -> "OK" }
}