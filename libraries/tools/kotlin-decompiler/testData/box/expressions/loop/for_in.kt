fun box(): String {
    for (i in 1..10) {
        println("Step: $i")
    }
    for (i in 1..10 step 2) {
        println("Step: $i")
    }
    for (i in 1 until 10) {
        println("Step: $i")
    }
    for (i in 1 until 10 step 2) {
        println("Step: $i")
    }
    for (i in 10 downTo 1) {
        println("Step: $i")
    }
    for (i in 10 downTo 1 step 2) {
        println("Step: $i")
    }

    var result = ""
    val t = listOf("O", "K")
    for (s in t) {
        result += s
    }
    return result
}