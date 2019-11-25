// FILE: /nameConflictStasExampleDecl.kt
package ru.spbau.mit.decl



open  class A {
}

// FILE: /nameConflictStasExampleInv.kt
package ru.spbau.mit.inv

import ru.spbau.mit.decl.A

class B {
    open  class A {
    }
    class C : A() {
    }
    class D : A() {
        val c : B.C = C()
    }
    class E : A() {
        val b : B = B()
        val a : B.A = B.A()
    }
}
fun box() : String  {
    val a : A = A()
    val a1 : B.A = B.A()
    val c : B.C = C()
    val e : B.E = E()
    return "OK"
}
