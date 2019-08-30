// !WITH_NEW_INFERENCE
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: negative):
 *  - expressions, when-expression -> paragraph 9 -> sentence 2
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 6 -> sentence 5
 *  - annotations, annotation-targets -> paragraph 1 -> sentence 1
 */
class A

fun test(a: Any): String {
    val q: String? = null

    when (a) {
        is A -> q!!
    }
    // When is not exhaustive
    return <!TYPE_MISMATCH!>q<!>
}
