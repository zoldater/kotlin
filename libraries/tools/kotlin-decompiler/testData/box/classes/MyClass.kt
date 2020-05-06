data class MyClass(val x: Int = 42, val y: Int) {
    fun foo() {}
    val t: Double = 0.0
}

interface TestInterface {
    fun foo(): Int
    fun bar(x: Int): Int
}

enum class Clazz {
    A, B, C, D
}

enum class Clzz(r: Int, g: Int, b: Int) {
    RED(255, 0, 0),
    GREEN(0, 255, 0),
    BLUE(0, 0, 255)
}

abstract class AClass

class TestAClass : TestInterface {
    override fun foo(): Int = 42
    override fun bar(x: Int): Int {
        return x + foo()
    }
}

val withInit: Int = 5

val woutInit: Int
    get() = 5

var withSet: Int = 5
    set(value) {
        field = value + 5
    }

fun box() = "OK"