interface FooInterface {
    val x: Int
    fun foo()
}

open class FooParent(override val x: Int) : FooInterface {
    override fun foo() {}
}

class FooDerived : FooParent {
    constructor(x: Int) : super(x)
    constructor() : super(42)
}

fun box(): String {
    val parent = FooParent(42)
    val derived = FooDerived(42)
    return if (parent.x == derived.x) "OK" else "FAIL"
}