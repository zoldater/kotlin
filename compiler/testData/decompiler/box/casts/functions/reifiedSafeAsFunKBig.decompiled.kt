fun fn0() {
}

fun fn1(x0: Any) {
}

fun fn2(x0: Any, x1: Any) {
}

fun fn3(x0: Any, x1: Any, x2: Any) {
}

fun fn4(x0: Any, x1: Any, x2: Any, x3: Any) {
}

fun fn5(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any) {
}

fun fn6(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any) {
}

fun fn7(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any) {
}

fun fn8(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any) {
}

fun fn9(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any) {
}

fun fn10(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any) {
}

fun fn11(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any) {
}

fun fn12(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any) {
}

fun fn13(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any) {
}

fun fn14(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any) {
}

fun fn15(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any) {
}

fun fn16(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any) {
}

fun fn17(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any) {
}

fun fn18(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any) {
}

fun fn19(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any, x18: Any) {
}

fun fn20(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any, x18: Any, x19: Any) {
}

fun fn21(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any, x18: Any, x19: Any, x20: Any) {
}

fun fn22(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any, x18: Any, x19: Any, x20: Any, x21: Any) {
}

val fns : Array<Any> = arrayOf<Any>(::fn0, ::fn1, ::fn2, ::fn3, ::fn4, ::fn5, ::fn6, ::fn7, ::fn8, ::fn9, ::fn10, ::fn11, ::fn12, ::fn13, ::fn14, ::fn15, ::fn16, ::fn17, ::fn18, ::fn19, ::fn20, ::fn21, ::fn22)
inline fun <T> reifiedSafeAsReturnsNonNull(x: Any?, operation: String) {
    val y : T? = try {
    (x as? T)}
catch (e : Throwable)  {
    throw AssertionError("${operation}: should not throw exceptions, got ${e}")
}
    if (y == null) {
        throw AssertionError("${operation}: should return non-null, got null")
    }
}

inline fun <T> reifiedSafeAsReturnsNull(x: Any?, operation: String) {
    val y : T? = try {
    (x as? T)}
catch (e : Throwable)  {
    throw AssertionError("${operation}: should not throw exceptions, got ${e}")
}
    if (y != null) {
        throw AssertionError("${operation}: should return null, got ${y}")
    }
}

interface TestFnBase {
    abstract  fun testGood(x: Any)
    abstract  fun testBad(x: Any)
}
object TestFn0: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<() -> Any?>(x, "x as? Function0<*>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<() -> Any?>(x, "x as? Function0<*>")
    }

}
object TestFn1: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?) -> Any?>(x, "x as? Function1<*, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?) -> Any?>(x, "x as? Function1<*, *>")
    }

}
object TestFn2: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?) -> Any?>(x, "x as? Function2<*, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?) -> Any?>(x, "x as? Function2<*, *, *>")
    }

}
object TestFn3: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?) -> Any?>(x, "x as? Function3<*, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?) -> Any?>(x, "x as? Function3<*, *, *, *>")
    }

}
object TestFn4: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function4<*, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function4<*, *, *, *, *>")
    }

}
object TestFn5: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function5<*, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function5<*, *, *, *, *, *>")
    }

}
object TestFn6: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function6<*, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function6<*, *, *, *, *, *, *>")
    }

}
object TestFn7: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function7<*, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function7<*, *, *, *, *, *, *, *>")
    }

}
object TestFn8: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function8<*, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function8<*, *, *, *, *, *, *, *, *>")
    }

}
object TestFn9: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function9<*, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function9<*, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn10: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function10<*, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function10<*, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn11: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function11<*, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function11<*, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn12: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn13: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn14: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn15: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn16: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn17: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn18: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn19: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn20: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn21: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
object TestFn22: TestFnBase {
    override fun testGood(x: Any) {
        return reifiedSafeAsReturnsNonNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

    override fun testBad(x: Any) {
        return reifiedSafeAsReturnsNull<(Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Any?>(x, "x as? Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    }

}
val tests : Array<TestFnBase> = arrayOf<TestFnBase>(TestFn0, TestFn1, TestFn2, TestFn3, TestFn4, TestFn5, TestFn6, TestFn7, TestFn8, TestFn9, TestFn10, TestFn11, TestFn12, TestFn13, TestFn14, TestFn15, TestFn16, TestFn17, TestFn18, TestFn19, TestFn20, TestFn21, TestFn22)
fun box() : String  {
    val tmp0_iterator : IntIterator = 0.rangeTo(22).iterator()
    while (tmp0_iterator.hasNext()) {
        val fnI : Int = tmp0_iterator.next()
        val tmp1_iterator : IntIterator = 0.rangeTo(22).iterator()
        while (tmp1_iterator.hasNext()) {
            val testI : Int = tmp1_iterator.next()
            if (fnI == testI) {
                tests.get(testI).testGood(fns.get(fnI))
            }
            else {
                tests.get(testI).testBad(fns.get(fnI))
            }
        }
    }
    return "OK"
}
