inline fun bar(f: () -> Unit) {
    f()
}

fun foo() {
    bar {
        Array(2) { false }
        Unit
    }
}
