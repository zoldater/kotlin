class MyClass

fun MyClass.printOk() = ok

val MyClass.ok: String
    get() = "OK"

fun box(): String = MyClass().printOk()