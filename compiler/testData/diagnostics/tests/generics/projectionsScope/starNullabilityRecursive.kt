// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
interface A<T : A<T?>?> {
    fun foo(): T?
}
fun testA(a: A<*>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("A<*>?")!>a.foo()<!> checkType { _<A<*>?>() }
}
