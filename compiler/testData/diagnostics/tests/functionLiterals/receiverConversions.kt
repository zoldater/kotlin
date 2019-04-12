// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER
// !WITH_NEW_INFERENCE

class A
class B

fun <K> id(x: K): K = x

fun test() {
    val l0: A.() -> Unit = { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>s: A<!> -> } // Error
    val l1: A.() -> Unit = id { s: A -> } // Good
    val l2: A.() -> Unit = id<A.() -> Unit> { <!OI;EXPECTED_PARAMETERS_NUMBER_MISMATCH!>s: A<!> ->  } // Error in OI
    val l3: A.() -> Unit = id<A.() -> Unit> { } // Good
    val l4: A.() -> Unit = <!OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>id { }<!> // Error in OI
    val l5: A.(A) -> Unit = id { a1: A, a2: A -> Unit } // Good
    val l6: A.(B) -> Unit = id { a: A, b: B -> Unit } // Good
}