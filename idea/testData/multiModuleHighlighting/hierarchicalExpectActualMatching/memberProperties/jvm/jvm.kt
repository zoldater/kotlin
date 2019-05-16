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

actual sealed class Case7 {
    actual <!INAPPLICABLE_LATEINIT_MODIFIER("is not allowed on properties with initializer")!>lateinit<!> var x: Any = 10
}

actual sealed class Case8 {
    actual var x: Any = 10
}

actual object Case9 : Comparable<Int> {
    actual override fun compareTo(other: Int): Int = TODO()
    actual const val x = 10
}

actual object Case10 {
    actual val x: Int = 10

    actual object Foo1 {
        actual val x = 10
        actual object Foo {
            actual val x: Int = 10
            actual object Foo {
                actual val x = 10
            }
        }
    }

    actual class Foo2 {
        actual val x = 10
    }

    actual sealed class Foo3<T> {
        actual val x: T = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>
    }

    actual interface Foo4<K> {
        actual val x: K
        // UNEXPECTED BEHAVIOUR: KT-30065
        actual fun <K><!ACTUAL_WITHOUT_EXPECT("Actual function 'bar'", " The following declaration is incompatible because modality is different:     public abstract expect fun <K> bar(): K#1 (type parameter of sample.Case10.Foo4.bar) ")!>bar<!>() = null <!UNCHECKED_CAST("Nothing?", "K")!>as K<!>
    }

    actual annotation class Foo5<T> {
        actual interface Foo<K> {
            actual val x: K
            // UNEXPECTED BEHAVIOUR: KT-30065
            actual fun <K><!ACTUAL_WITHOUT_EXPECT("Actual function 'bar'", " The following declaration is incompatible because modality is different:     public abstract expect fun <K> bar(): K#1 (type parameter of sample.Case10.Foo5.Foo.bar) ")!>bar<!>() = null <!UNCHECKED_CAST("Nothing?", "K")!>as K<!>
        }
    }
}

actual annotation class Case11<T>(actual val x: Int)
