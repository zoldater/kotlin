package sample

actual val <T> T.case_1 by lazy { 10 <!UNCHECKED_CAST("Int", "T")!>as T<!> }

actual val case_2: List<Int> get() = listOf(1)
