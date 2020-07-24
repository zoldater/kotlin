import Clazz.*

sealed class Clazz(val x: Int) {
    object ClazzZero : Clazz(0) {}
    object ClazzTwo : Clazz(2) {}
}

fun <T : Clazz> T.isLessThenTwo(): Boolean {
    return x < 2
}

val <T : Clazz> T.isPositive: Boolean
    get() = x > 0

fun box(): String {
    val callExtResult = ClazzZero.isLessThenTwo()
    val classRef = Clazz::isLessThenTwo
    val callRefResult = classRef(ClazzZero)
    val objectRef = ClazzZero::isLessThenTwo
    val objectRefResult = objectRef()


    val extPropResult = ClazzTwo.isPositive
    val classPropRef = Clazz::isPositive
    val extPropRefResult = classPropRef(ClazzTwo)
    val objectPropRef = ClazzTwo::isPositive
    val objPropRefResult = objectPropRef()

    return if (callExtResult && callRefResult && objectRefResult
        && extPropResult && extPropRefResult && objPropRefResult
    ) "OK" else "FAIL"

}