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


    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(project(":compiler:cli"))
    testCompile(projectTests(":compiler:tests-common"))
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
