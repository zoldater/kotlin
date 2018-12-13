// !LANGUAGE: +ProhibitSelfReferenceInSuperConstructorCallOfObject

open class ImplicitProp(val prop: Int) {
    companion object : ImplicitProp(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT!>prop<!>)
}
