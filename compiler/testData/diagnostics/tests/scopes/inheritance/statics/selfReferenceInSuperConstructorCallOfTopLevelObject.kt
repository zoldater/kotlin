// !LANGUAGE: -ProhibitSelfReferenceInSuperConstructorCallOfObject

open class Foo(val x: Int)

object MyObject : Foo(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>MyObject.x<!>)