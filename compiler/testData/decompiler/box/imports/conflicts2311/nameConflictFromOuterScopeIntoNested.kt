// FILE: nameConflictFromOuterScopeIntoNestedDecl.kt
package ru.spbau.mit.declaration

open class ClassName {
    open fun foo() = "OK"
}

// FILE: nameConflictFromOuterScopeIntoNestedInv.kt
package ru.spbau.mit.invocation

import ru.spbau.mit.declaration.ClassName as ConflictedClassName

class ClassName : ConflictedClassName() {
    override fun foo() = "FAIL"
}

fun box(): String {
    val className = ClassName()
    val conflictedClass = ConflictedClassName()
    if (className.foo() == conflictedClass.foo()) {
        return className.foo()
    } else {
        return conflictedClass.foo()
    }
}