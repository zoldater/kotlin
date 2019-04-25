
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":examples:scripting-jvm-simple-script"))
    compile(project(":kotlin-scripting-jvm-host"))
    compile(project(":kotlin-script-util"))
    testRuntimeOnly(projectRuntimeJar(":kotlin-compiler"))
    testRuntimeOnly(kotlinReflect())
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
