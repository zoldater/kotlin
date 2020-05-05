interface TestInterface {
    fun foo()
    fun bar(x: Int)
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

abstract class TestAClass : TestInterface

val withInit: Int = 5

val woutInit: Int
    get() = 5

var withSet: Int = 5
    set(value) {
        field = value + 5
    }