// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
expect open class A {
    constructor(s: String)

    constructor(n: Number) : this("A")
}

expect class B : A {
    <!INAPPLICABLE_CANDIDATE!>constructor(i: Int)<!>

    constructor() : super("B")
}
