fun test1(a: Int, b: Int) = a + b

fun test1x(a: Int, b: Int): Int {
    return a.plus(b)
}

fun test2(a: Int, b: Int) = a - b

fun test2x(a: Int, b: Int): Int {
    return a.minus(b)
}

fun test3(a: Int, b: Int) = a * b

fun test3x(a: Int, b: Int): Int {
    return a.times(b)
}

fun test4(a: Int, b: Int) = a / b

fun test4x(a: Int, b: Int): Int {
    return a.div(b)
}

fun box(): String {
    val t1 = test1(2, 1)
    val t1x = test1x(2, 1)
    val t2 = test2(2, 1)
    val t2x = test2x(2, 1)
    val t3 = test3(2, 1)
    val t3x = test3x(2, 1)
    val t4 = test4(2, 1)
    val t4x = test4x(2, 1)
    val z1 = t3 > t3x
    val z2 = t3 >= t3x
    val z3 = t3 < t3x
    val z4 = t3 <= t3x
    val z5 = t3 == t3x
    val z6 = t3 != t3x
    val z7 = t3 === t3x
    val z8 = t3 !== t3x
    var x = 0
    x += t1
    x *= t2
    x /= t3
    x -= t4
    when (t1) {
        3 -> return "OK"
        else -> return "FAIL"
    }

}