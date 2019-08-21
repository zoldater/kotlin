
open class A {
    open fun foo(): Int = 1
}

class B : A() {
    override fun foo(): Int = 2
}

fun box(): String {
    val a: A = B()
    a.foo()
    // if (a.foo() != 2)
    //     return "Fail"

    return "OK"
}