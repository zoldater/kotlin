interface Base {
    fun message(): String
}

class BaseImpl(val x: String) : Base {
    override fun message() = x
}

class Derived(b: Base) : Base by b {
    override fun message(): String = "OK"
}

fun box(): String {
    val b = BaseImpl("FAIL")
    return Derived(b).message()
}