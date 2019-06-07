package case1

expect interface A {
    fun commonFooA()
}

expect interface B : A {
    fun commonFooB()
}