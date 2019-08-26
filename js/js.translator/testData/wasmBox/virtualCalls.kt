
open class A {
    open fun foo(): Int = 1
}

class B : A() {
    override fun foo(): Int = 2
}

fun box(): String {
    val a: A = B()
    val res = a.foo()
    if (res != 2)
         return "Fail $res"

    return "OK"
}