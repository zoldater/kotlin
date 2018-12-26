// !DIAGNOSTICS: -UPPER_BOUND_VIOLATED

//interface C0<T, S : C0<*, S>>
//
//interface C1<T : C1<T, *>, <!FINITE_BOUNDS_VIOLATION!>S : C1<S, *><!>>   // T -> S, S -> S
//interface C2<<!FINITE_BOUNDS_VIOLATION!>T : C2<T, *><!>, S : C2<*, S>>   // T -> S, S -> T

//interface D1<<!FINITE_BOUNDS_VIOLATION!>T<!>, U> where T : U, U: D1<*, U>
interface D2<<!FINITE_BOUNDS_VIOLATION!>T<!>, U> where T : U?, U: D2<*, *>
//interface D3<<!FINITE_BOUNDS_VIOLATION!>T<!>, U, V> where T : U, U : V, V: D3<*, *, V>

//interface A<T, U> where T : A<U, T>, U: A<T, A<in U, T>>

Computing all supertypes for D2
Computed all supertypes for D2
Starting disconnection for D2
Computing all supertypes for Any
Computed all supertypes for Any
Finished disconnection for D2

Starting disconnection for Any
Finished disconnection for Any

T:
1. Computing all supertypes for T + Computed all supertypes for T
2. Starting disconnection for T
        Computing all supertypes for U
            (Got recursion for U)
            Starting disconnection for U
                Got recursion for U
            Finished disconnection for U
        Computed all supertypes for U
Finished disconnection for T