package sample

expect class <!AMBIGUOUS_ACTUALS("Class 'A1'", "common-2, common-1")!>Case1<!> {
    val <!AMBIGUOUS_ACTUALS("Property 'x'", "common-2, common-1")!>x<!>: Number
}

actual class Case2 {
    actual val x = 10 as Number
}

expect annotation class <!NO_ACTUAL_FOR_EXPECT("annotation class 'A2'", "common-2", "")!>Case3<!> {
    annotation class Foo
}

expect class <!NO_ACTUAL_FOR_EXPECT("class 'A3'", "common-2", "")!>Case4<!> {
    class Foo
}

expect val <!AMBIGUOUS_ACTUALS("Property 'x'", "common-2, common-1")!>case_5<!>: Int

actual val case_6 = 10
