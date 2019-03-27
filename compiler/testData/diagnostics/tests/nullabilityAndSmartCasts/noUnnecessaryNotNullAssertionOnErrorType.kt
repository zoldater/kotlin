// !WITH_NEW_INFERENCE

package a

fun foo() {
    bar()!!
}

fun bar() = <!UNRESOLVED_REFERENCE!>aa<!>