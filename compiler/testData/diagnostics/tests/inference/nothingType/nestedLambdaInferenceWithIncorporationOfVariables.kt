// !LANGUAGE: +NewInference

fun <K> id1(k: K): K = k
fun <V> id2(v: V): V = v

fun test() {
    <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>id1<!> {
        <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER, IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>id2<!> <!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>{
            3
        }<!>
    }
}
