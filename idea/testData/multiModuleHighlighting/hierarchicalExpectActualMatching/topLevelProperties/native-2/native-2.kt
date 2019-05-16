package sample

actual val <T> T.case_1 get() = 10 <!UNCHECKED_CAST("Int", "T")!>as T<!>

actual val case_2: List<Int> get() = null <!CAST_NEVER_SUCCEEDS!>as<!> List<Int>

actual val case_3: Nothing = null!!

actual val <T> MutableList<out T>.case_4 get() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>

actual var <T> Map<in T, <!REDUNDANT_PROJECTION("Map")!>out<!> T>.case_5
    get() = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>
    set(<!UNUSED_PARAMETER("value")!>value<!>) {}

actual inline var Number.case_6
    get() = 10
    set(<!UNUSED_PARAMETER("value")!>value<!>) {}

actual inline val <T> T.case_7 get() = 10 <!UNCHECKED_CAST("Int", "T")!>as T<!>

actual var case_8 = 10

actual inline val <reified T> T.case_9 get() = null as T

actual val <T> T.case_10 get() = 10

actual val case_11 = object : Foo {}

actual inline var <T> T.case_12
    get() = object : Foo {}
    set(<!UNUSED_PARAMETER("value")!>value<!>) {}

class Case9 : Foo {}

actual val case_13: Foo = object: Foo {}

actual val case_14 = 10 as Number
