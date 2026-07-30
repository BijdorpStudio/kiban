import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compat.tapmoc)
    alias(libs.plugins.maven.publish)
}

group = "nl.bijdorpstudio.kiban"
version = "0.4.0"

tapmoc {
    java(libs.versions.java.version.get().toInt())
    kotlin(libs.versions.kotlin.version.get())
}

kotlin {
    jvm()
    androidLibrary {
        namespace = "nl.bijdorpstudio.kiban"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosX64()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    linuxX64()
    linuxArm64()
    mingwX64()
    js(IR) {
        nodejs()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.assertk)
            implementation(libs.kotlin.test)
        }
    }
}


mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "kiban", version.toString())

    pom {
        name = "Kiban"
        description = "Kotlin Multiplatform IBAN Library."
        inceptionYear = "2025"
        url = "https://github.com/BijdorpStudio/kiban"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "barend"
                name = "Barend Garvelink"
                url = "https://github.com/barend"
            }
            developer {
                id = "emartynov"
                name = "Eugen Martynov"
                url = "https://github.com/emartynov"
            }
            developer {
                id = "bijdorpstudio"
                name = "Bijdorp Studio"
                url = "https://bijdorpstudio.nl"
            }
        }
        scm {
            url = "https://github.com/BijdorpStudio/kiban"
            connection = "scm:git:git://github.com/BijdorpStudio/kiban.git"
            developerConnection = "scm:git:ssh://git@github.com:BijdorpStudio/kiban.git"
        }
    }
}
