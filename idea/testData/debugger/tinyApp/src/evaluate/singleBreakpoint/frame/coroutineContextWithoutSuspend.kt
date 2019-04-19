package coroutineContextWithoutSuspend

import kotlin.coroutines.coroutineContext

suspend fun main() {
    foo()
}

private suspend fun foo() {
    //Breakpoint!
    val a = 5
}

// EXPRESSION: coroutineContext
// RESULT: 'coroutineContext' is not available