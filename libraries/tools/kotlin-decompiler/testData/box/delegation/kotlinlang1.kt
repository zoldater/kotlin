interface Base {
    fun base(): Int
}

class BaseImpl(val x: Int) : Base {
    override fun base() = x
}

class Derived(b: Base) : Base by b

fun box(): String {
    val b = BaseImpl(10)
    return when (Derived(b).base()) {
        10 -> "OK"
        else -> "FAIL"
    }
}