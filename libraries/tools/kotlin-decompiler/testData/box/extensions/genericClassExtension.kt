class Clazz<T>(val x: T)

fun Clazz<Int>.isPositive(): Boolean = x > 0

val Clazz<String>.isEmpty: Boolean
    get() = x.isEmpty()

fun box(): String {
    val intClazz = Clazz(42)
    val intCallExtResult = intClazz.isPositive()
    val extFunIntRef = Clazz<Int>::isPositive
    val callRefResult = extFunIntRef(intClazz)

    val stringClazz = Clazz("OK")
    val stringExtPropResult = !stringClazz.isEmpty
    val stringClazzPropRef = Clazz<String>::isEmpty
    val stringClazzPropRefResult = !stringClazzPropRef(stringClazz)

    return if (intCallExtResult && callRefResult && stringExtPropResult && stringClazzPropRefResult) stringClazz.x else "FAIL"
}