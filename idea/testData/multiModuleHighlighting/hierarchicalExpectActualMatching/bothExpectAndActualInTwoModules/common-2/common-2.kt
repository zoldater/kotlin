package sample

actual class Case1 {
    actual val x = 10 as Number
}

expect class Case2 {
    actual val x = 10 as Number
}

expect val x: Int

actual val <!AMBIGUOUS_EXPECTS("Actual property 'x'", "common-2, common-1")!>x<!> = 10

actual annotation class Case3 {
    actual annotation class Foo
}

actual class Case4 {
    actual class Foo
}

actual val case_5 = 10

expect val case_6: Int
