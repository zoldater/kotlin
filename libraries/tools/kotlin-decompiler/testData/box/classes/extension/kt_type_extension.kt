fun Int.box(other: Int): String {
    if (this > other) {
        return foo
    } else {
        return "FAIL"
    }
}

val Int.foo: String
    get() = "OK"

fun box(): String = 99.box(9)