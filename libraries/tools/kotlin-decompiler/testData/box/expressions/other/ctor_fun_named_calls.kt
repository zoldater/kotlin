fun foo1(x: Int) = x

fun foo2(x: Int, y: Int) = x + y

fun bar(x: Int = 0, y: Int = 42) = x + y

fun barLeft(y: Int) = bar(y = y)

fun barRight(x: Int) = bar(x = x)

fun barBoth() = bar(y = 0, x = 42)

data class DataClazz(val x: Int = 0, val y: Int = 42)

fun box(): String {
    val onlyY = DataClazz(y = 1)
    val onlyX = DataClazz(x = 1)
    val both = DataClazz(x = 1, y = 2)
    val mirrored = DataClazz(y = 2, x = 1)
    return "OK"
}