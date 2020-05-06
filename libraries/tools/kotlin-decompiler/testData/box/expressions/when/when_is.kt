fun bar() = 666

fun box(): String {
    val f = bar()
    when {
        (f >= 800 && f <= 1000) -> return "FAIL"
        f is Int -> return "OK"
        else -> return "FAIL"
    }
}