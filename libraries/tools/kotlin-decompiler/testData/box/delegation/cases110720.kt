interface Base {
    fun getValue(): String
    fun test(): String = getValue()
}

interface Foo : Base {
    fun foo()
}

interface Bar {
    val x: Int
    fun bar()
}

val barImpl = object : Bar {
    override val x: Int = 1
    override fun bar() {}
}

class Fail : Base {
    override fun getValue() = "OK"
}

class Derived : Base by Fail(), Foo, Bar by barImpl {
    override fun foo() {}
}

fun box(): String {
    return Derived().test()
}
