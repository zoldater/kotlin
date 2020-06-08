fun whenVal(x: Int): String {
    return when (val odd = x % 2) {
        1 -> "OK"
        else -> "FAIL"
    }
}

fun whenSubject(x: Number): String {
    return when (x) {
        55 -> "FAIL"
        is Short -> "FAIL"
        in listOf(11, 22, 33, 44, 55) -> "FAIL"
        else -> "OK"
    }
}

fun box() = whenSubject(3)