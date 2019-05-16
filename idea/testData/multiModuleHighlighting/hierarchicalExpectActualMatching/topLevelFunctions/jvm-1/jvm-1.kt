package sample

actual fun case_1(): List<Int> = listOf(1)

actual fun case_2(): Nothing = null!!

actual fun <T> MutableList<out T>.case_3() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>

actual fun <T> Map<in T, <!REDUNDANT_PROJECTION("Map")!>out<!> T>.case_4() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>
