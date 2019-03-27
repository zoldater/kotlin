// !WITH_NEW_INFERENCE

fun f() {
    foo {
        <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> {
            <!RETURN_NOT_ALLOWED!>return@foo<!> 1
        }
        return@foo 1
    }
}

fun foo(<!UNUSED_PARAMETER!>a<!>: Any) {}
fun bar(<!UNUSED_PARAMETER!>a<!>: Any) {}