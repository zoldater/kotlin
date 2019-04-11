// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// !WITH_NEW_INFERENCE


// FILE: Inv.java

public abstract class Inv<C extends Inv<C>> {
    @org.jetbrains.annotations.NotNull
    public C replace() { return null; }
}

// FILE: main.kt

fun <K> id(x: K): K = x

fun <K> select(x: K, y: K): K = x

fun test(context: Inv<*>) {
    // context.replace(): (Inv<*>..Inv<*>?)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>")!>context<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>")!>id(context)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(Inv<*>..Inv<*>?)")!>context.replace()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(Inv<*>..Inv<*>?)")!>id(context.replace())<!>

    val newContext = select(<!DEBUG_INFO_EXPRESSION_TYPE("(Inv<*>..Inv<*>?)")!>context.replace()<!>, context)
    <!DEBUG_INFO_EXPRESSION_TYPE("(Inv<*>..Inv<*>?)")!>newContext<!>
}