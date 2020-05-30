fun whenClosure(x: Int): String {
    return when (x) {
        55 -> "FAIL"
        is String -> "FAIL"
        in listOf(1, 2, 3, 4, 5) -> "OK"
        else -> "FAIL"
    }
}

fun box() = whenRegular(3)