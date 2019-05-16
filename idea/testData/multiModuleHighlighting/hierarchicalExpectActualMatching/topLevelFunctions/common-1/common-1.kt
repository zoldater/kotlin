package sample

expect fun case_1(): List<Int>

expect fun case_2(): Nothing

expect fun <T> MutableList<out T>.case_3(): T

expect fun <T> Map<in T, <!REDUNDANT_PROJECTION("Map")!>out<!> T>.case_4(): T
