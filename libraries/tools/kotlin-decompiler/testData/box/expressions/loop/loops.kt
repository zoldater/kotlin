fun box(): String {
    var i = 10
    while (i > 0) {
        i--
        --i
    }

    do {
        i++
        ++i
    } while (i <= 10)

    for (j in 0..i) {
        val t = j + 1
    }

    return "OK"
}