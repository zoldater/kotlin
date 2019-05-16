package sample

expect class Case1<T> : Iterable<T>

expect class Case2<T> : Iterable<T>

expect annotation class Case3

expect annotation class Case4 {
    annotation class Foo(val x: Int) {
        sealed class Bar<T> : Iterable<T>, Comparable<T> {
            val x: T
        }
    }
}

expect <!EXPERIMENTAL_FEATURE_WARNING("The feature "inline classes" is experimental")!>inline<!> class Case5<T>(val x: Int): Comparable<T>

expect class Case6<T>() {
    inner class Foo<T>() {

    }
}
