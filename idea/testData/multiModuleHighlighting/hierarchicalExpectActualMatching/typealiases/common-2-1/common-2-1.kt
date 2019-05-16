package sample

expect interface Case7<T, K> : Comparable<K> where K: T, T: Iterable<K>

expect object Case8 : Comparable<Int>

expect class Case9<T> : Iterable<T> {
    override fun iterator(): Nothing
}

expect class Case10<T> : Iterable<T> {
    override fun iterator(): Nothing
    fun bar(): Nothing
}
