// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.contracts.*

val String?.asNonNull: String
    get() {
        <!CONTRACT_NOT_ALLOWED(Contracts are not allowed yet for property accessors)!>contract<!> {
            returns() implies (this@asNonNull != null)
        }

        return this!!
    }

var Boolean.foo: Boolean
    get() {
        return this
    }

    set(value) {
        <!CONTRACT_NOT_ALLOWED(Contracts are not allowed yet for property accessors)!>contract<!> {
            returns() implies value
        }
        if (!value) throw Exception()

        return
    }

fun test1(x: String?) {
    val l1 = x.asNonNull.length
    val l2 = x<!UNSAFE_CALL!>.<!>length
}

fun test2(x: Any?) {
    true.foo = x is String
    x.<!UNRESOLVED_REFERENCE!>length<!>
}