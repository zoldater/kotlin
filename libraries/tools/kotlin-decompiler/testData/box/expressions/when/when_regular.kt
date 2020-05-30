fun whenRegular(x: Number): String {
    return when {
        x == 55 -> "FAIL"
        x is Short -> "FAIL"
        x !in listOf(11, 22, 33, 44, 55) -> "FAIL"
        x in listOf(1, 2, 3, 4, 5) || x is Int -> "OK"
        else -> "FAIL"
    }
}

fun box() = whenRegular(44)