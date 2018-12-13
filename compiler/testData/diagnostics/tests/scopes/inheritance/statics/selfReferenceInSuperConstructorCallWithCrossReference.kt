// !LANGUAGE: -ProhibitSelfReferenceInSuperConstructorCallOfObject

open class Bar(val x: Int)

open class Foo1 {
    companion object : Bar(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>Foo1.x<!>)
}

open class Foo2 {
    companion object : Bar(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>Foo2.Companion.x<!>)
}

open class Foo3 {
    companion object : Bar(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>Companion.x<!>)
}

open class Foo4 {
    object MyObject : Bar(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>MyObject.x<!>)
}

open class Foo5 {
    companion object : Bar(<!SELF_REFERENCE_IN_SUPER_CONSTRUCTOR_CALL_OF_OBJECT_WARNING!>Foo5.prop<!>) {
        val prop = 10
    }
}