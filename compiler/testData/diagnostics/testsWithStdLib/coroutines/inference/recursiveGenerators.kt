// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

@file:UseExperimental(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class GenericController<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(@BuilderInference g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

val test1 = generate {
    yield(generate <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>{
        yield(generate <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>{
            yield(generate <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>{
                yield(3)
            }<!>)
        }<!>)
    }<!>)
}
