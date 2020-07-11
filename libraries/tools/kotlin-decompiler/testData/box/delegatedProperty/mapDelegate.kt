class TestSession(val map: Map<String, Any?>) {
    val number: Int by map
    val result: String by map
}

fun box(): String {
    val session = TestSession(mapOf("number" to 42, "result" to "OK"))
    return session.result
}