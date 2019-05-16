package sample

expect object Case10 {
    val x: Int

    object Foo1 {
        val x: Int
        object Foo {
            val x: Int
            object Foo {
                val x: Int
            }
        }
    }

    class Foo2 {
        val x: Int
    }

    sealed class Foo3<T> {
        val x: T
    }

    interface Foo4<K> {
        val x: K
        fun <K> bar(): K
    }

    annotation class Foo5<T> {
        interface Foo<K> {
            val x: K
            fun <K>bar(): K
        }
    }
}
