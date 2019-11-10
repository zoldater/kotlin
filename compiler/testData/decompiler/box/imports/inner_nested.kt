// FILE: InnerNestedDeclarations.kt
package ru.spbau.mit.declaration

object TopLevelObject {
    val prop = 1
    fun topLevelFoo() {}
}

class Outer {
    inner class Inner {
    }

    class Nested {
    }

    companion object {
        val companionProp = 1
        fun companionNonameFoo() {}
    }

    object NamedObject {
        val namedObjectProp = 1
        fun namedObjectFoo() {}
    }
}

class WithNamedCompanion {
    companion object {
        val namedCompanionProp = 1
        fun companionNamedFoo() {}
    }
}
// FILE: InnerNestedInvocations.kt
package ru.spbau.mit.invocation


import ru.spbau.mit.declaration.Outer
import ru.spbau.mit.declaration.Outer.Inner
import ru.spbau.mit.declaration.Outer.Nested
import ru.spbau.mit.declaration.TopLevelObject
import ru.spbau.mit.declaration.WithNamedCompanion

fun box(): String {
    val outer: Outer = Outer()
    val nested: Nested = Nested()
    val inner: Inner = outer.Inner()
    val companionProp: Int = Outer.companionProp
    val prop: Int = TopLevelObject.prop
    val namedCompanionProp: Int = WithNamedCompanion.namedCompanionProp
    WithNamedCompanion.companionNamedFoo()
    Outer.companionNonameFoo()
    TopLevelObject.topLevelFoo()
    Outer.NamedObject.namedObjectFoo()
    return "OK"
}
