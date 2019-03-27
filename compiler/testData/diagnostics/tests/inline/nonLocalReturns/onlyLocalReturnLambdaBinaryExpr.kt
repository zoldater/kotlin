// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
// !WITH_NEW_INFERENCE

class Z {
    inline infix fun <R> inlineFun(crossinline p: () -> R) {
        p()
    }
}

fun <R> fun1(p: () -> R) {
    Z() inlineFun {
        p()
    }
}

fun <R> fun3(p: () -> R) {
    Z() <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>inlineFun<!> {
        <!RETURN_NOT_ALLOWED!>return<!>;
    }
}

fun <R> fun4(p: () -> R) {
    Z() inlineFun lambda@ {
        return@lambda p();
    }
}
