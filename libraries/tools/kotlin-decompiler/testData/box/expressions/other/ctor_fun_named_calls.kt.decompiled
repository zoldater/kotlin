// FILE: /ctor_fun_named_calls.kt

fun foo1(x: Int): Int {
    return x
}

fun foo2(x: Int, y: Int): Int {
    return x + y
}

fun bar(x: Int = 0, y: Int = 42): Int {
    return x + y
}

fun barLeft(y: Int): Int {
    return bar(y = y)
}

fun barRight(x: Int): Int {
    return bar(x = x)
}

fun barBoth(): Int {
    return bar(42, 0)
}

data class DataClazz(val x: Int = 0, val y: Int = 42)

fun box(): String {
    val onlyY = DataClazz(y = 1)
    val onlyX = DataClazz(x = 1)
    val both = DataClazz(1, 2)
    val mirrored = DataClazz(1, 2)
    return "OK"
}
