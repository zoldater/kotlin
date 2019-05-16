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
    override fun compareTo(other: T) = null!!
}

actual typealias Case5<T> = Case5_1<T>

class Case6_1<T>() {
    inner class Foo<T>() {}
}

// UNEXPECTED BEHAVIOUR
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS("Case6_1", "      public final expect inner class Foo<T>      The following declaration is incompatible because some expected members have no actual ones:         public final inner class Foo<T>     No actual members are found for expected members listed below:          public constructor Foo<T>()          The following declaration is incompatible because return type is different:             public constructor Foo<T>() ")!>Case6<!><T> = Case6_1<T>


interface Foo<K> : Iterable<K>

class Case14_1<T> : Foo<T> {
    override fun iterator() = null!!
}

actual typealias <!ACTUAL_WITHOUT_EXPECT("Actual typealias 'Case14'", " The following declaration is incompatible because some supertypes are missing in the actual declaration:     public final expect class Case14<T> : Iterable<T> ")!>Case14<!><T> = Case14_1<T>

class Case15_1<T> : Iterable<Int> {
    override fun iterator() = null!!
}

actual typealias <!ACTUAL_WITHOUT_EXPECT("Actual typealias 'Case15'", " The following declaration is incompatible because some supertypes are missing in the actual declaration:     public final expect class Case15<T> : Iterable<T> ")!>Case15<!><T> = Case15_1<T>

expect enum class Case16_1 {}
actual enum class Case16_1 {}

actual typealias Case16 = Case16_1

enum class Case17_1 { TEST }

actual typealias Case17 = Case17_1

enum class Case18_1 { TEST2 }

actual typealias <!ACTUAL_WITHOUT_EXPECT("Actual typealias 'Case18'", " The following declaration is incompatible because some entries from expected enum are missing in the actual enum:     public final expect enum class Case18 : Enum<Case18> ")!>Case18<!> = Case18_1
