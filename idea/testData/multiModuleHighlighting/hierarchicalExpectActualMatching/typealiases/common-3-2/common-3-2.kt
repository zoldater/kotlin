package sample

expect enum class Case19 {
    TEST
}

expect enum class Case20 {
    TEST;
    val x: Int
}

expect enum class Case21 {
    TEST
}

expect data class Case22

expect data class Case23<R>

expect sealed class Case24

expect sealed class Case25<T>

expect open class Case26<T> where T : Comparable<T>

expect abstract class Case27<T : Iterable<<!REDUNDANT_PROJECTION("Iterable")!>out<!> T>>
