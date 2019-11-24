// FILE: nameConflictFromOuterScopeIntoNestedDecl.kt
package ru.spbau.mit.declaration

open class ClassName {
    open fun foo() = "OK"
}

// FILE: nameConflictFromOuterScopeIntoNestedInv.kt
package ru.spbau.mit.invocation

open class ClassName : ru.spbau.mit.declaration.ClassName() {
    override fun foo() = "FAIL"
}

class NoConflictClass : ClassName() {
    override fun foo() = "OK"
}

fun box(): String {
    val className = ClassName()
    val conflictedClass = ru.spbau.mit.declaration.ClassName()
    val noConflictClass = NoConflictClass()
    return noConflictClass.foo()
}