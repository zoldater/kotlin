// !WITH_NEW_INFERENCE
fun test() {
    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>run<!> {<!RETURN_NOT_ALLOWED!>return<!>}
    <!NI;UNREACHABLE_CODE!>run {}<!>
}

fun test2() {
    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>run<!> {<!RETURN_NOT_ALLOWED!>return@test2<!>}
    <!NI;UNREACHABLE_CODE!>run {}<!>
}

fun test3() {
    fun test4() {
        <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>run<!> {<!RETURN_NOT_ALLOWED!>return@test3<!>}
        <!NI;UNREACHABLE_CODE!>run {}<!>
    }
}

fun <T> run(f: () -> T): T { return f() }
