fun foo() = 42

fun box(): String {
    val f = foo()
    when (f) {
        is Int, is Number -> return "OK"
        in 1..10, in 1000..2000 -> return "FAIL"
        1, in 100..200, is Int -> return "OK"
        12, 13 -> return "FAIL"
        22, 23, 24 -> return "FAIL"
        32, 33, 34, 35 -> return "FAIL"
        else -> return "FAIL"
    }
}