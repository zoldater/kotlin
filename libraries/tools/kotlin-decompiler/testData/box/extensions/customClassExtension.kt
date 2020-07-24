class Clazz(val x: Int)

fun Clazz.isLessThenTwo(): Boolean {
    return x < 2
}

val Clazz.isPositive: Boolean
    get() = x > 0

fun box(): String {
    val funClazz = Clazz(0)
    val callExtResult = funClazz.isLessThenTwo()
    val classRef = Clazz::isLessThenTwo
    val callRefResult = classRef(funClazz)
    val instanceRef = funClazz::isLessThenTwo
    val instanceRefResult = instanceRef()

    val propClazz = Clazz(2)

    val extPropResult = propClazz.isPositive
    val classPropRef = Clazz::isPositive
    val extPropRefResult = classPropRef(propClazz)
    val instancePropRef = propClazz::isPositive
    val instancePropRefResult = instancePropRef()

    return if (callExtResult && callRefResult && instanceRefResult
        && extPropResult && extPropRefResult && instancePropRefResult) "OK" else "FAIL"

}