// !WTIH_NEW_INFERENCE
// Related issue: KT-28370

fun test1() {
    var x: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>
    x = ""

    try {
        x = null
    } catch (e: Exception) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length // smartcast shouldn't be allowed (OOME could happen after `x = null`)
        throw e
    }
    finally {
        // smartcast shouldn't be allowed, `x = null` could've happened
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    // smartcast shouldn't be allowed, `x = null` could've happened
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun test2() {
    var t2: Boolean? = true
    if (t2 != null) { // or `t2 is Boolean`
        try {
            throw Exception()
        } catch (e: Exception) {
            t2 = null
        }
        <!DEBUG_INFO_SMARTCAST!>t2<!>.not() // wrong smartcast, NPE
    }
}

fun test3() {
    var t2: Boolean? = true
    if (t2 != null) { // or `t2 is Boolean`
        try {
            t2 = null
        } finally { }
        <!DEBUG_INFO_SMARTCAST!>t2<!>.not() // wrong smartcast, NPE
    }
}