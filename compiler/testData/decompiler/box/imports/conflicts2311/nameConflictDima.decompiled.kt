// FILE: /bar.kt
package bar

import foo.invoke

fun foo(a: foo.A) {
    a.invoke(1)
}
fun A(x: String) : foo.A  {
    return A()
}

// FILE: /foo.kt
package foo



class A {
    fun foo() {
    }

}
operator fun A.invoke(x: Int) : Int  {
    return x
}
fun test() {
    val a1 : A = A()
    val a2 : A = A("")
}
fun box() : String  {
    return "OK"
}
