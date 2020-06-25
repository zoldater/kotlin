// FILE: /default_named_args.kt

fun topLevelWithDefault(def: String = "DEFAULT"): Int {
    return def.length
}

fun topLevelMixed(def: String = "DEFAULT", nonDef: Int): Boolean {
    return def.length == nonDef
}

fun topLevel2Mixed(def1: String = "DEF1", def2: String = "DEF2", nonDef: Int): Boolean {
    return def1.length + def2.length > nonDef
}

class ClassWithDefaultArgsMethod(str: String) {
    val str: String = str
    fun withDefaultCtorArg(defArg: String = this.str): Boolean {
        return defArg == this.str
    }

    fun withDefaultArgMixed(defArg: String = "ARG", nonDefArg: String): Boolean {
        return defArg == this.str || nonDefArg == this.str
    }
}

fun box(): String {
    val var1 = topLevelWithDefault()
    val var2 = topLevelWithDefault("ANOTHER")
    val var3 = topLevelWithDefault("ANOTHER")
    if (var1 != var2 || var2 != var3) {
        return "FAIL"
    }
    val onlyNonDefNamed = topLevelMixed(nonDef = 7)
    val bothNonamed = topLevelMixed("SOME", 4)
    val bothNamed = topLevelMixed("NAMED", 5)
    val bothNamedReordered = topLevelMixed("ANSWER", 6)
    if (!onlyNonDefNamed && bothNamed && bothNonamed && bothNamedReordered) {
        return "FAIL"
    }
    val mixed21 = topLevel2Mixed(nonDef = 0)
    val mixed22 = topLevel2Mixed(def1 = "NONDEF1", nonDef = 0)
    val mixed23 = topLevel2Mixed(def2 = "NONDEF2", nonDef = 0)
    val mixed24 = topLevel2Mixed("NONDEF1", "NONDEF2", 0)
    if (!mixed21 && mixed22 && mixed23 && mixed24) {
        return "FAIL"
    }
    val withDefaultArgsMethodInstance = ClassWithDefaultArgsMethod("DEF")
    val boolDef = withDefaultArgsMethodInstance.withDefaultCtorArg()
    val boolDefChanged = !withDefaultArgsMethodInstance.withDefaultCtorArg("NONE")
    val boolDefChanged2 = !withDefaultArgsMethodInstance.withDefaultCtorArg("NONE")
    val boolNamed = withDefaultArgsMethodInstance.withDefaultArgMixed("NONE", "DEF")
    val boolNonDefNamed = withDefaultArgsMethodInstance.withDefaultArgMixed(nonDefArg = "DEF")
    val boolBothNonamed = withDefaultArgsMethodInstance.withDefaultArgMixed("NONE", "DEF")
    if (!boolDef && boolDefChanged && boolDefChanged2 && boolNamed && boolNonDefNamed && boolBothNonamed) {
        return "FAIL"
    }
    return "OK"
}
