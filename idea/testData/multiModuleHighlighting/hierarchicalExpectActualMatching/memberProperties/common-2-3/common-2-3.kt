package sample

expect sealed class Case7 {
    <!EXPECTED_LATEINIT_PROPERTY!>lateinit<!> var x: Any
}

expect sealed class Case8 {
    var x: Any
}

expect object Case9 : Comparable<Int> {
    override fun compareTo(other: Int): Int
    // UNEXPECTED BEHAVIOUR: KT-18856
    <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val x: Int
}
