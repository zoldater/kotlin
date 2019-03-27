// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

fun test() {
    data class Pair<F, S>(val first: F, val second: S)
    val (x, y) =
            Pair(1,
                 if (1 == 1)
                     <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>Pair<String, String>::first<!>
                 else
                     <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>Pair<String, String>::second<!>)
}