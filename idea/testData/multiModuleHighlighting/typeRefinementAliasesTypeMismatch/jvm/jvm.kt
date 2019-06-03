// ________JVM________

@file:Suppress("ACTUAL_WITHOUT_EXPECT")

package exceptionAliases

actual typealias MyException =
        java.util.concurrent.CancellationException

actual open class OtherException : IllegalStateException()

fun test() {
    cancel(CancellationException()) // TYPE_MISMATCH

    other(OtherException())
}
