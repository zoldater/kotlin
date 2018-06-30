fun consume(<!UNUSED_PARAMETER!>x<!>: Any?) {}

fun test() {
    var name: String?
    name = "Test"
    consume(if (true) { name = null; 2}  else 1) // name assigning null here
    name<!UNSAFE_CALL!>.<!>hashCode() // smart cast to not-null
}
