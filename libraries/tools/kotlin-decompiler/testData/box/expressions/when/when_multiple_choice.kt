fun foo() = 42

fun box(): String {
    val f = foo()
    when (f) {
        12, 13 -> return "FAIL"
        22, 23, 24 -> return "FAIL"
        32, 33, 34, 35 -> return "FAIL"
        else -> return "OK"
    }
}