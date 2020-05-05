plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    google()
}

dependencies {
    implementation(project(":kotlin-reflect-api"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":idea:formatter"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:fir:tree:tree-generator"))


    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompileOnly(intellijDep())
    testRuntimeOnly(intellijDep())

    testCompileOnly(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testCompileOnly(projectTests(":compiler:tests-common"))
    testCompileOnly(projectTests(":compiler:fir:analysis-tests"))
    testRuntimeOnly(project(":core:descriptors.runtime"))

    Platform[192].orHigher {
        testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
        testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    }



    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(project(":compiler:cli"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(project(":idea:ide-common"))
    testImplementation(project(":idea:idea-jps-common"))

    testImplementation(project(":compiler:cli-common"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testImplementation(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()
