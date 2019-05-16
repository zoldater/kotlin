package sample

actual fun case_10() = object : I {}

actual <!NOTHING_TO_INLINE("public actual inline fun <T> T.case_11(): I defined in sample in file jvm-3.kt")!>inline<!> fun <T> T.case_11() = object : I {}

class Case12 : I {}

actual fun case_12(): I = Case12()

actual fun case_13(): Number = 10

actual suspend fun case_15() = 10

actual suspend inline fun <T> (suspend T.(T) -> T).case_16(crossinline x: (T) -> T): T = x(<!NO_VALUE_FOR_PARAMETER("p1")!>)<!>

actual suspend inline fun <T> (T.(T) -> T).case_17(crossinline x: (T) -> T): T = x(<!NO_VALUE_FOR_PARAMETER("p1")!>)<!>

actual suspend inline fun <T> (suspend T.(T) -> T).case_18(crossinline x: (T) -> T): T = x(<!NO_VALUE_FOR_PARAMETER("p1")!>)<!>

actual suspend fun <T> (suspend T.(T) -> T).case_19(x: (T) -> T): T = x(<!NO_VALUE_FOR_PARAMETER("p1")!>)<!>

actual fun <T> T.case_8() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>

actual fun case_9() = 10

actual fun <T> Map<in T, <!REDUNDANT_PROJECTION("Map")!>out<!> T>.case_4() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>
