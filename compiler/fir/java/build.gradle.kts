
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:frontend.common"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:javac-wrapper"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}