fun topLevelOk(): String = "OK"

class Clazz {
    fun instanceOk(): String = ::topLevelOk.let { it() }

    companion object {
        fun staticOk(): String = ::topLevelOk.let { it() }
    }
}

object ObjWithOk {
    fun okFromObject(): String = Clazz.Companion::staticOk.let { it() }
}

fun box(): String {
    val clazzCtorRef = ::Clazz
    val clazzInstance = clazzCtorRef()

    val instanceOk = clazzInstance::instanceOk
    val staticOk = Clazz.Companion::staticOk
    val topLevelOk = ::topLevelOk
    val objOk = ObjWithOk::okFromObject

    return "OK".takeIf { listOf(instanceOk, staticOk, topLevelOk, objOk).map { it() }.all { it == "OK" } } ?: "FAIL"

}