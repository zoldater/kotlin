package sample

actual <!NOTHING_TO_INLINE("public actual inline fun Number.case_5(): Int defined in sample in file jvm-2.kt")!>inline<!> fun Number.case_5() = 10

actual <!NOTHING_TO_INLINE("public actual inline fun <T> T.case_14(): T defined in sample in file jvm-2.kt")!>inline<!> fun <T> T.case_14() = 10 <!UNCHECKED_CAST("Int", "T")!>as T<!>

actual fun case_7() = 10

actual <!NOTHING_TO_INLINE("public actual inline fun <T> T.case_8(): T defined in sample in file jvm-2.kt")!>inline<!> fun <T> T.case_8() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>

actual fun case_9() = 10

actual fun <T> Map<in T, <!REDUNDANT_PROJECTION("Map")!>out<!> T>.case_4() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>