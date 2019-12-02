//FILE: bar.kt
package bar

import foo.A
import foo.invoke

fun foo(a: A) {
    a(1)
}

fun A(x: String): A = A()

// FILE: foo.kt
package foo
import bar.A

class A {
    fun foo() {}
}

operator fun A.invoke(x: Int): Int = x
fun test() {
    val a1 = A()
    val a2 = A("")
}

fun box() = "OK"
