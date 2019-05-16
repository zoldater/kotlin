package sample

actual class Case1 {
    actual val x = 0f as Number
}

val <T> T.x2 get() = 0 <!UNCHECKED_CAST("Int", "T")!>as T<!>

actual class Case2<K> {
    actual var x = (0 <!UNCHECKED_CAST("Int", "K")!>as K<!>).x2
}

actual class Case3<K> : Iterable<K> {
    actual override fun iterator() = TODO()
    actual protected val y = null <!UNCHECKED_CAST("Nothing?", "K")!>as K<!>
}

actual enum class Case4 {
    TEST, TEST1;
    actual val x = 10
}

actual enum class Case5(actual val x: Int)

actual enum class Case6 constructor(actual val x: Int)

actual annotation class Case11<T>(actual val x: Int)
