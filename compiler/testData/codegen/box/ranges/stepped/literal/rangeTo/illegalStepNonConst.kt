// WITH_RUNTIME
import kotlin.test.*

fun zero() = 0

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        for (i in 1..7 step zero()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 1L..7L step zero().toLong()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 'a'..'g' step zero()) {
        }
    }

    return "OK"
}