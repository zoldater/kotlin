// !LANGUAGE: +NewInference
// WITH_RUNTIME

fun <K> select(x: K, y: K): K = x

fun box(): String {
    val x = 10
    val y = select(20, "20")

    if (y == x + 10) {
        return "OK"
    }
    return "Error"
}