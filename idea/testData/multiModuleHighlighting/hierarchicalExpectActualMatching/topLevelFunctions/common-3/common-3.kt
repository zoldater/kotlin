package sample

actual <!NOTHING_TO_INLINE("public actual inline fun Number.case_5(): Int defined in sample in file common-3.kt")!>inline<!> fun Number.case_5() = 10

interface I

expect fun case_10(): I

expect fun <T> T.case_11(): I

expect fun case_12(): I

expect fun case_13(): Number

actual <!NOTHING_TO_INLINE("public actual inline fun <T> T.case_14(): T defined in sample in file common-3.kt")!>inline<!> fun <T> T.case_14() = 10 <!UNCHECKED_CAST("Int", "T")!>as T<!>

expect suspend fun case_15(): Int

expect suspend inline fun <T> (suspend T.(T) -> T).case_16(crossinline x: (T) -> T): T

expect suspend inline fun <T> (T.(T) -> T).case_17(crossinline x: (T) -> T): T

expect suspend inline fun <T> (suspend T.(T) -> T).case_18(crossinline x: (T) -> T): T

expect suspend fun <T> (suspend T.(T) -> T).case_19(x: (T) -> T): T

actual fun case_7() = 10

