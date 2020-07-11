interface Base {
    val message: String
    fun answer(): String
}

class BaseImpl(val x: String) : Base {
    override val message = x
    override fun answer(): String = message
}

class Derived(b: Base) : Base by b {
    override val message = "FAIL"
}

fun box(): String {
    val b = BaseImpl("OK")
    val derived = Derived(b)

    return derived.answer()
}