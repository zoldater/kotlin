//@file:Suppress(U)

package sample

actual interface Input

class <!JS_FAKE_NAME_CLASH("head", "val head: Int", "val head: Int")!>JSInput<!> : AbstractInput()

// ------------------------------------

expect class ExpectInJsActualInJs
actual class ExpectInJsActualInJs