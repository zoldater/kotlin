// ________COMMON________

@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package exceptionAliases

expect open class MyException :
    IllegalStateException

fun cancel(cause: MyException) {}



expect open class OtherException :
    IllegalStateException

fun other(cause: OtherException) {}
