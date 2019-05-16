package sample

actual fun case_1(): List<Int> = listOf(1)

actual fun case_2(): Nothing = null!!

expect fun Number.case_5(): Int

expect inline fun <T> T.case_14(): T

expect fun case_7(): Int

expect fun <T> T.case_8(): T

expect fun case_9(): Int

actual fun <T> MutableList<out T>.case_3() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>
