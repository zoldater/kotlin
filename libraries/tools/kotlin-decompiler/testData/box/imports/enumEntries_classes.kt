// FILE: EnumClassDeclarations.kt
package ru.spbau.mit

enum class En { N, A, B, C }

enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}

class Z1(val x: En)
class Z2(val z: Z1)
class ZN(val z: Z1?)

// FILE: EnumClassInvocations.kt
package ru.spbau.mit

import ru.spbau.mit.Color.*
import ru.spbau.mit.En
import ru.spbau.mit.En.*
import ru.spbau.mit.Z1
import ru.spbau.mit.Z2
import ru.spbau.mit.ZN


fun wrap1(x: En): Z1? = if (x.ordinal == 0) null else Z1(x)
fun wrap2(x: En): Z2? = if (x.ordinal == 0) null else Z2(Z1(x))
fun wrapN(x: En): ZN? = if (x.ordinal == 0) null else ZN(Z1(x))

fun box(): String {
    val n = En.N
    val a = En.A

    if (wrap1(n) != null) throw AssertionError()
    if (wrap1(a) == null) throw AssertionError()
    if (wrap1(a)!!.x != a) throw AssertionError()

    if (wrap2(n) != null) throw AssertionError()
    if (wrap2(a) == null) throw AssertionError()
    if (wrap2(a)!!.z.x != a) throw AssertionError()

    if (wrapN(n) != null) throw AssertionError()
    if (wrapN(a) == null) throw AssertionError()
    if (wrapN(a)!!.z!!.x != a) throw AssertionError()

    val blue = Color.BLUE
    if (blue.rgb != 255) throw AssertionError()

    return "OK"
}