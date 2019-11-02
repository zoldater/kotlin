class Box<T>(val value: T) {
}
fun <T> run(vararg z: T) : Box<T>  {
    return Box<T>< T >(z.get(0))
}

fun box() : String  {
    val b : Box<Long> = run<Long>(-1, -1, -1)
    val expected : Long? = -1
    return if (b.value == expected) {
    "OK"
}
else {
    "fail"
}
}
