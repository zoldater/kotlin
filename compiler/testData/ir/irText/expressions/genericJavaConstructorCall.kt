// DUMP_EXTERNAL_CLASS J
// FILE: genericJavaConstructorCall.kt
fun test1() = J(123, 456)
fun test2() = J(123, "abc")
fun test3() = J<Number>(123, 456)

// FILE: J.java
public class J {
    public <T> J(T x, T y) {
    }
}
