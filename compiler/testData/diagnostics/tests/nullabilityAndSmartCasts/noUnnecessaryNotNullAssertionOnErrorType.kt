// !WITH_NEW_INFERENCE

package a

fun foo() {
    bar()<!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>!!<!>
}

fun bar() = <!UNRESOLVED_REFERENCE!>aa<!>