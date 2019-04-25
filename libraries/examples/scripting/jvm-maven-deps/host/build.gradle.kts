
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":examples:scripting-jvm-maven-deps"))
    compile(project(":kotlin-scripting-jvm-host"))
    compile(kotlinStdlib())
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":compiler:util"))
    runtime(kotlinReflect())

    testRuntimeOnly(projectRuntimeJar(":kotlin-compiler"))
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
