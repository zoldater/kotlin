// !WITH_NEW_INFERENCE

package n

fun <T> foo(<!UNUSED_PARAMETER!>t<!>: T, <!UNUSED_PARAMETER!>t1<!>: T) {}

fun test() {
    //no type inference error
    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!UNRESOLVED_REFERENCE!>aaab<!>, <!UNRESOLVED_REFERENCE!>bbb<!>)
}