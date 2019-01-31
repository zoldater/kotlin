// !WITH_NEW_INFERENCE
// !LANGUAGE: +NewDataFlowForTryExpressions
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
        <!OI;DEBUG_INFO_SMARTCAST!>s<!><!NI;UNSAFE_CALL!>.<!>length
        requireNotNull(s2)
    }
    <!OI;DEBUG_INFO_SMARTCAST!>s<!><!NI;UNSAFE_CALL!>.<!>length
    <!NI;DEBUG_INFO_SMARTCAST!>s1<!><!OI;UNSAFE_CALL!>.<!>length
    <!DEBUG_INFO_SMARTCAST!>s2<!>.length
}