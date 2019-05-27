@file:Suppress("UNSUPPORTED_FEATURE")

package foo

expect interface A {
    fun commonFun()
}

class CommonGen<T : A> {
    val a: T get() = null!!
}

class List<out T>(val value: T)

fun getList(): List<A> = null!!