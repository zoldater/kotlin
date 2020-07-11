var result = ""

val lazyValue: String by lazy {
    result = "OK"
    "Hello"
}

fun box(): String {
    val x = lazyValue
    return result
}