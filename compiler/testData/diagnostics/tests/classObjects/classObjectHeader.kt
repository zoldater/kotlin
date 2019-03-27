// !WITH_NEW_INFERENCE

package test

open class ToResolve<SomeClass>(<!UNUSED_PARAMETER!>f<!> : (Int) -> Int)
fun testFun(<!UNUSED_PARAMETER!>a<!> : Int) = 12

class TestSome<P> {
    companion object : <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>ToResolve<<!UNRESOLVED_REFERENCE!>P<!>><!>({testFun(it)}) {
    }
}