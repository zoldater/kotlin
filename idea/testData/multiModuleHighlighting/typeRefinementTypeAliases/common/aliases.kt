@file:Suppress("UNSUPPORTED_FEATURE")

package aliases

expect interface A {
    fun commonFun()
}

typealias A1 = A

expect interface B {
    fun commonFun()
}

typealias B1 = B

class CommonInv<T>(val value: T)