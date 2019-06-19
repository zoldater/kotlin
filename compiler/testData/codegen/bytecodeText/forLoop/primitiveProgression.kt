// IGNORE_BACKEND: JVM_IR
fun f() {
    for (i in 0..5 step 2) {
    }

    for (i in 5 downTo 1 step 1) { // suppress optimized code generation for 'for-in-downTo'
    }
}

// JVM non-IR does NOT specifically handle "step" progressions. The stepped progressions in the above code are constructed and their
// first/last/step properties are retrieved.
// JVM IR has an optimized handler for "step" progressions and elides the construction of the stepped progressions.

// 0 iterator
// 2 getFirst
// 2 getLast
// 2 getStep