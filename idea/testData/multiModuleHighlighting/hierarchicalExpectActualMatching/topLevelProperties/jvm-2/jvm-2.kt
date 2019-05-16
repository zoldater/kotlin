package sample

actual val <T> T.case_1 by lazy { 10 <!UNCHECKED_CAST("Int", "T")!>as T<!> }

actual val case_2: List<Int> get() = listOf(1)

actual val case_3: Nothing = null!!

actual val <T> MutableList<out T>.case_4 get() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>
