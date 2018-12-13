// !LANGUAGE: -ProhibitSelfReferenceInSuperConstructorCallOfObject
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class ImplicitProp(val prop: Int) {
    companion object : ImplicitProp(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>prop<!>)
}

open class ExplicitCompanionProp(val prop: Int) {
    companion object Cmp : ExplicitCompanionProp(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>Cmp.prop<!>)
}

open class ExplicitThisProp(val prop: Int) {
    companion object : ExplicitThisProp(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>this.prop<!>)
}

open class ImplicitFunction(val i: Int) {
    fun func(): Int = i

    companion object : ImplicitFunction(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>func()<!>)
}

open class ExplicitCompanionFunction(val i: Int) {
    fun func(): Int = i

    companion object Cmp : ExplicitCompanionFunction(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>Cmp.func()<!>)
}

open class ExplicitThisFunction(val i: Int) {
    fun func(): Int = i

    companion object : ExplicitThisFunction(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>this.func()<!>)
}

open class ImplicitReceiverForInner(a: Any) {
    inner class Inner

    companion object : ImplicitReceiverForInner(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>Inner()<!>)
}

// Similar but correct examples

open class Expr(val prop: Int) {
    companion object : Expr(42)
}

open class CheckNested(a: Any) {
    class Nested

    companion object : CheckNested(Nested())
}

open class Delayed(val f: () -> Int, val prop: Int) {
    companion object : Delayed({ prop }, 42)
}