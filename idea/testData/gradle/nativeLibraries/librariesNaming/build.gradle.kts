
buildscript {
    extra["kotlinPluginVersion"] = "1.3.21"
    apply(from = "setKotlinPluginVersion.gradle")
    val kotlinPluginVersion = extra["kotlinPluginVersion"] as String
}

plugins {
    kotlin("multiplatform") version kotlinPluginVersion
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js()
    macosX64()
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }

        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        js().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}
