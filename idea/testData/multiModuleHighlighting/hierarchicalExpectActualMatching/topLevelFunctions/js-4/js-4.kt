package sample

actual fun <T> Map<in T, <!REDUNDANT_PROJECTION("Map")!>out<!> T>.case_4() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>

actual fun <T> T.case_8() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>

actual fun case_9() = 10

actual suspend inline fun <T> (T.(T) -> T).case_17(crossinline x: (T) -> T): T = x(<!NO_VALUE_FOR_PARAMETER("p1")!>)<!>

actual suspend inline fun <T> (suspend T.(T) -> T).case_18(x: (T) -> T): T = x(<!NO_VALUE_FOR_PARAMETER("p1")!>)<!>

actual tailrec fun case_20(y: () -> Unit): Int {
    y()
    return if (true) 10 else case_20 { }
}

actual infix fun <T> T.case_21(x: Int) = 10

actual infix fun <T> T.case_22(x: Int) = 10

actual infix fun <T> T.case_23(x: Int) = 10

actual operator fun CharSequence.plus(x: Int) = 10

actual internal suspend inline infix operator fun <T> T.plus(x: () -> T) = x()

actual operator infix fun <T> T.minus(x: () -> T) = x()

actual public fun case_24() = 10

actual fun case_25() = 10
