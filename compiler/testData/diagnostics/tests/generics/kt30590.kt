// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION
// !LANGUAGE: +NewInference
// !CHECK_TYPE
// FULL_JDK

// ---------------- errorUpperBoundConstraint ----------------

// FILE: Sam.java

public interface Sam<K> {
    Sam.Result<K> compute();

    public static class Result<V> {
        public static <V> Sam.Result<V> create(V value) {}
    }
}

// FILE: Foo.java

public class Foo {
    public static <T> void foo(Sam<T> var1) {
    }
}

// FILE: test.kt

fun test(e: <!UNRESOLVED_REFERENCE!>ErrorType<!>) {
    Foo.foo {
        Sam.Result.create(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>e<!>)
    }
}

// ---------------- inferenceForElvis ----------------

fun <T: Any> foo(f: (T) -> Unit): T? = null // T is used only as return type
fun test() {
    val x = foo { it } ?: "" // foo() is inferred as foo<String>, which isn't very good
    val y: Any = foo { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>it<!> } ?: "" // but for now it's fixed by specifying expected type
    val z = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> { it }
    val zz: Any? = foo { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>it<!> }
}

//// ---------------- collectorInference ----------------

interface Data
interface Item
class FlagData(val value: Boolean) : Data
class ListData<I : Item>(val list: List<I>) : Data

fun <T> listOf(vararg items: T): List<T> = TODO()

fun test2x(): Data = if (true)
    <!DEBUG_INFO_EXPRESSION_TYPE("ListData<Item>")!>ListData(listOf())<!>
    else <!DEBUG_INFO_EXPRESSION_TYPE("FlagData")!>FlagData(true)<!>

// ---------------- some stuff ----------------

interface A

fun <T : A, R> emptyStrangeMap2(t: T): Map<T, R> where R : T = throw Exception("$t")

fun test5(a: A) {
    emptyStrangeMap2(a)
}