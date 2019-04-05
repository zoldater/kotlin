// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -REDUNDANT_PROJECTION -UNUSED_EXPRESSION
// !LANGUAGE: +NewInference
// FILE: main.kt

class Out<out T> {
    fun get(): T = TODO()
}

fun case_2(a: Out<out Out<out Out<out Out<out Out<out Out<out Int?>?>?>?>?>?>?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out Out<out Out<out Out<out Out<out Out<out kotlin.Int?>?>?>?>?>?>?")!>a<!>
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out Out<out Out<out Out<out Out<out Out<out kotlin.Int?>?>?>?>?>?> & Out<out Out<out Out<out Out<out Out<out Out<out kotlin.Int?>?>?>?>?>?>?")!>a<!>
        val b = a.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<Out<kotlin.Int?>?>?>?>?>?")!>b<!>
    }
}