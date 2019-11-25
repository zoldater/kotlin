// FILE: nameConflictStasExample.kt
package ru.spbau.mit

open class A

class B {
    open class A
    class C : A()
    class D : B.A() {
        val c = C()
    }

    class E : ru.spbau.mit.A() {
        val b = B()
        val a = A()
    }
}

fun box(): String {
    val a = A()
    val a1 = B.A()
    val c = B.C()
    val e = B.E()
    return "OK"

}