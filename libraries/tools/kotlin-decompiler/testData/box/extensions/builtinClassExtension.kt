fun Int.isLessThenTwo(): Boolean {
    return this < 2
}

val Int.isPositive: Boolean
    get() = this > 0

fun box(): String {
    val callExtResult = 0.isLessThenTwo()
    val classRef = Int::isLessThenTwo
    val callRefResult = classRef(0)
    val instanceRef = 0::isLessThenTwo
    val instanceRefResult = instanceRef()

    val extPropResult = 2.isPositive
    val classPropRef = Int::isPositive
    val extPropRefResult = classPropRef(2)
    val instancePropRef = 2::isPositive
    val instancePropRefResult = instancePropRef()

    return if (callExtResult && callRefResult && instanceRefResult
        && extPropResult && extPropRefResult && instancePropRefResult) "OK" else "FAIL"

}