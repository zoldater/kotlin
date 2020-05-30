fun whenRegular(x: Int): String {
    return when (val odd = x % 2) {
        0 -> "FAIL"
        1 -> "OK"
        else -> "FAIL"
    }
}

fun box() = whenRegular(5)