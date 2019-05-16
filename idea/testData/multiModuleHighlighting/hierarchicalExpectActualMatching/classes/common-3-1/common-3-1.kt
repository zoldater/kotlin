package sample

expect annotation class Case24

expect annotation class Case25 {
    annotation class Foo(val x: Int) {
        sealed class Bar<T> : Iterable<T>, Comparable<T> {
            val x: T
        }
    }
}

actual enum class Case6 {
    TEST
}

actual enum class Case7 {
    TEST, TEST2, TEST3;
    fun bar() {}
}

actual enum class Case8(val x: Int) {
    TEST(1)
}