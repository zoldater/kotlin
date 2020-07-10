interface IBar {
    val x: Int
    fun foo()
}

object ObjImplIface : IBar {
    override val x: Int = 33
    override fun foo() {}
}

val objImplIface = object : IBar {
    override val x: Int
        get() = TODO("Not yet implemented")

    override fun foo() {
        TODO("Not yet implemented")
    }
}

class CompanionImplIface {
    companion object : IBar {
        override val x: Int = 42
        override fun foo() {}
    }
}

open class IClazz {
    open fun clazzFun() {}
}

object ObjExtClazz : IClazz() {
    override fun clazzFun() {
        super.clazzFun()
    }
}

val objExtClazz = object : IClazz() {
    override fun clazzFun() {
        super.clazzFun()
    }
}

class CompanionExtClazz {
    companion object : IClazz() {
        override fun clazzFun() {}
    }
}

open class IClazzCtor(val x: Int) {
    open fun clazzCtorFun() {}
}

object ObjExtClazzCtor : IClazzCtor(42) {
    override fun clazzCtorFun() {
        super.clazzCtorFun()
    }
}

val objExtClazzCtor = object : IClazzCtor(42) {
    override fun clazzCtorFun() {
        super.clazzCtorFun()
    }
}

class CompanionExtClazzCtor {
    companion object : IClazzCtor(42) {
        override fun clazzCtorFun() {
            super.clazzCtorFun()
        }
    }
}

abstract class IClazzCtorDef(val x: Int = 42) {
    abstract val y: Int
    abstract fun clazzCtorDefFun()
}

object ObjExtClazzCtorDef : IClazzCtorDef() {
    override val y: Int
        get() = TODO("Not yet implemented")

    override fun clazzCtorDefFun() {
        TODO("Not yet implemented")
    }
}

val objExtClazzCtorDef = object : IClazzCtorDef() {
    override val y: Int
        get() = TODO("Not yet implemented")

    override fun clazzCtorDefFun() {
        TODO("Not yet implemented")
    }
}

class CompanionExtClazzCtorDef {
    companion object : IClazzCtorDef() {
        override val y: Int = 11

        override fun clazzCtorDefFun() {}
    }
}

fun box() = "OK"