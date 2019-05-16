package sample

expect class Case3<T> : Iterable<T> {
    override fun iterator(): Nothing
    protected val y: T
}

expect enum class Case4 {
    TEST;
    val x: Int
}
