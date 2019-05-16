package sample

<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias <!ACTUAL_WITHOUT_EXPECT("Actual typealias 'Case1'", " The following declaration is incompatible because class kinds are different (class, interface, object, enum, annotation):     public final expect class Case1<T> : Iterable<T> ")!>Case1<!><T> = Iterable<T><!>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED("Class 'Case2_2'", "public abstract operator fun iterator(): Iterator<T> defined in kotlin.collections.Iterable")!>class Case2_2<!><T> : Iterable<T>
typealias Case2_1<T> = Case2_2<T>
<!ACTUAL_TYPE_ALIAS_NOT_TO_CLASS!>actual typealias Case2<T> = Case2_1<T><!>

annotation class Case3_1
actual typealias Case3 = Case3_1

annotation class Case4_1 {
    annotation class Foo(val x: Int) {
        sealed class Bar<T> : Iterable<T>, Comparable<T> {
            val x: T = null <!UNCHECKED_CAST("Nothing?", "T")!>as T<!>
        }
    }
}

// UNEXPECTED BEHAVIOUR
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS("Case4_1", "      public final expect annotation class Foo : Annotation      The following declaration is incompatible because some expected members have no actual ones:         public final annotation class Foo : Annotation     No actual members are found for expected members listed below:          public constructor Foo(x: Int)          The following declaration is incompatible because return type is different:             public constructor Foo(x: Int) ")!>Case4<!> = Case4_1

<!EXPERIMENTAL_FEATURE_WARNING("The feature "inline classes" is experimental")!>inline<!> class Case5_1<T>(val x: Int): Comparable<T> {
    override fun compareTo(other: T) = TODO()
}

actual typealias Case5<T> = Case5_1<T>

class Case6_1<T>() {
    inner class Foo<T>() {}
}

// UNEXPECTED BEHAVIOUR
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS("Case6_1", "      public final expect inner class Foo<T>      The following declaration is incompatible because some expected members have no actual ones:         public final inner class Foo<T>     No actual members are found for expected members listed below:          public constructor Foo<T>()          The following declaration is incompatible because return type is different:             public constructor Foo<T>() ")!>Case6<!><T> = Case6_1<T>

interface Case7_1<T, K : T> : Comparable<K> where T: Iterable<K>

actual typealias Case7<Y, P> = Case7_1<Y, P>

object Case8_1 : Comparable<Int> {
    override fun compareTo(other: Int) = 10
}

actual typealias Case8 = Case8_1

class Case9_1<T1> : Iterable<T1> {
    override fun iterator() = TODO()
}

actual typealias Case9<L> = Case9_1<L>

interface Case10_2 {
    fun bar(): Nothing = TODO()
}

class Case10_1<T1> : Iterable<T1>, Case10_2 {
    override fun iterator() = TODO()
}

actual typealias Case10<L> = Case10_1<L>

actual typealias Case11 = Nothing

class Case12_1<K> : List<K> {
    override val size: Int get() = TODO()
    override fun contains(element: K)= TODO()
    override fun containsAll(elements: Collection<K>)= TODO()
    override fun get(index: Int)= TODO()
    override fun indexOf(element: K)= TODO()
    override fun isEmpty()= TODO()
    override fun iterator()= TODO()
    override fun lastIndexOf(element: K)= TODO()
    override fun listIterator()= TODO()
    override fun listIterator(index: Int)= TODO()
    override fun subList(fromIndex: Int, toIndex: Int) = TODO()
}

actual typealias Case12<K> = Case12_1<K>

interface Foo<K> : Iterable<K>

class Case13_1<T : Any?> : Iterable<T>, Foo<T> {
    override fun iterator() = TODO()
}

actual typealias Case13<T> = Case13_1<T>

class Case14_1<T> : Iterable<T> {
    override fun iterator() = TODO()
}

actual typealias <!ACTUAL_WITHOUT_EXPECT("Actual typealias 'Case14'", " The following declaration is incompatible because some supertypes are missing in the actual declaration:     public final expect class Case14<T> : Iterable<T> ")!>Case14<!><T> = Case14_1<T>

class Case15_1<T> : Iterable<Int> {
    override fun iterator() = TODO()
}

actual typealias <!ACTUAL_WITHOUT_EXPECT("Actual typealias 'Case15'", " The following declaration is incompatible because some supertypes are missing in the actual declaration:     public final expect class Case15<T> : Iterable<T> ")!>Case15<!><T> = Case15_1<T>

expect enum class Case16_1 {}

actual typealias Case16 = Case16_1

enum class Case17_1 { TEST }

actual typealias Case17 = Case17_1

enum class Case18_1 { TEST2 }

actual typealias <!ACTUAL_WITHOUT_EXPECT("Actual typealias 'Case18'", " The following declaration is incompatible because some entries from expected enum are missing in the actual enum:     public final expect enum class Case18 : Enum<Case18> ")!>Case18<!> = Case18_1

actual typealias Case19 = Case20

actual enum class Case20 { TEST; actual val x = 10; }

enum class Case21_1(<!UNUSED_PARAMETER("x")!>x<!>: Int) { TEST(10) }

actual typealias Case21 = Case21_1

data class Case22_1(val x: Int)

actual typealias Case22 = Case22_1

data class Case23_1<K>(val x: K)

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS("Case23_1", "      public constructor Case23<R>(x: R)      The following declaration is incompatible because names of type parameters are different:         public constructor Case23_1<K>(x: K) ")!>Case23<!><T> = Case23_1<T>

sealed class Case24_1(val x: Int) : Iterable<Int> {
    override fun iterator() = TODO()
}

actual typealias Case24 = Case24_1

sealed class Case25_1<K> : Iterable<K> {
    abstract override fun iterator(): Iterator<K>
}

actual typealias Case25<Y> = Case25_1<Y>

open class Case26_1<T : Comparable<T>> constructor(var x: Int = 10)

actual typealias Case26<Y> = Case26_1<Y>

abstract class Case27_1<T : Iterable<T>>

actual typealias Case27<Y> = Case27_1<Y>

actual enum class Case16_1 {}
