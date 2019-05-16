package sample

actual val <T> T.case_1 by lazy { 10 <!UNCHECKED_CAST("Int", "T")!>as T<!> }

actual val case_2: List<Int> get() = listOf(1)

actual val case_3: Nothing = null!!

actual val <T> MutableList<out T>.case_4 get() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>

actual var <T> Map<in T, <!REDUNDANT_PROJECTION("Map")!>out<!> T>.case_5
    get() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>
    set(<!UNUSED_PARAMETER("value")!>value<!>) {}

actual inline var Number.case_6
    get() = 10
    set(<!UNUSED_PARAMETER("value")!>value<!>) {}

actual inline val <T> T.case_7 get() = 10 <!UNCHECKED_CAST("Int", "T")!>as T<!>
