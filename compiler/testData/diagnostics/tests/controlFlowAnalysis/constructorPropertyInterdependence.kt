// See KT-12809
open class A(val a: Any) {
    override fun toString() = a.toString()
}

object B : A(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>B.<!UNINITIALIZED_VARIABLE!>foo<!><!>) { // call B.foo should be not-allowed
    val foo = 4
}