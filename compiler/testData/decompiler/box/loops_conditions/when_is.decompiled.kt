fun bar(): Int {
    return 666
}

fun box(): String {
    val f: Int = bar()
    val tmp0_subject: Int = f
    when {
        (tmp0_subject is kotlin.Int).not() -> {
            return "FAIL"
        }
        (tmp0_subject is kotlin.Int) -> {
            return "OK"
        }
        else -> {
            return "FAIL"
        }
    }
}
