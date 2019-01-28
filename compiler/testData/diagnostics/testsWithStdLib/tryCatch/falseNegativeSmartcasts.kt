// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// Related issue: KT-28370

class ExcA : Exception()
class ExcB : Exception()

fun test1(s1: String?, s2: String?) {
    var s: String? = null
    s = ""
    try {
        s = null
        requireNotNull(s1)
    }
    catch (e: Exception) {
        return
    }
    finally {
        <!DEBUG_INFO_SMARTCAST!>s<!>.length
        requireNotNull(s2)
    }
    <!DEBUG_INFO_SMARTCAST!>s<!>.length
    s1<!UNSAFE_CALL!>.<!>length
    <!DEBUG_INFO_SMARTCAST!>s2<!>.length
}

