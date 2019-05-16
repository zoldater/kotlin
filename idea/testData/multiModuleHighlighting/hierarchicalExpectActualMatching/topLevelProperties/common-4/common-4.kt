package sample

expect var case_8: Int

// UNEXPECTED BEHAVIOUR: KT-31464
<!INLINE_PROPERTY_WITH_BACKING_FIELD!>expect inline val <reified T> T.case_9: T<!>
