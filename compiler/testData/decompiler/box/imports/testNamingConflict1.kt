// FILE: TestNamingConflict1.kt
package ru.spbau.mit

open class A {
    open class B
}

open class B {
    open class A : ru.spbau.mit.A()
    open class C : ru.spbau.mit.A.B()
    open class D : A()
}

open class C {
    class B : A.B()
    companion object {
        fun foo() = B()
    }
}

fun box(): String {
    class LocalChild : C() {
        fun bar() = foo()
    }

    val bar = LocalChild().bar()
    return if (bar is A.B) "OK" else "FAIL"
}