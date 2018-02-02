// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_VARIABLE

sealed class Bird {
    class Penguin : Bird()
    class Ostrich : Bird()
    class Kiwi : Bird()
}

fun <T: Bird> useInstanceInSealedHeirarchy(value: T) {
    val v = when(value) {
        is Bird.Penguin -> 1
        is Bird.Ostrich -> 2
        is Bird.Kiwi -> 3
    }

    when(value) {
        is Bird.Penguin -> 1
        is Bird.Ostrich -> 2
        is Bird.Kiwi -> 3
    }
}

fun <T : Bird> nonExhaustive(value: T) {
    val v = <!NO_ELSE_IN_WHEN!>when<!> (value) {
        is Bird.Penguin -> 1
        is Bird.Kiwi -> 3
    }

    <!NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS!>when<!> (value) {
        is Bird.Penguin -> 1
        is Bird.Ostrich -> 2
    }
}

fun <T : Bird> redundantEsle(value: T) {
    val v = when(value) {
        is Bird.Penguin -> 1
        is Bird.Ostrich -> 2
        is Bird.Kiwi -> 3
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 42
    }

    when(value) {
        is Bird.Penguin -> 1
        is Bird.Ostrich -> 2
        is Bird.Kiwi -> 3
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 42
    }
}