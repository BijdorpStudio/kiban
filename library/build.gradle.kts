import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compat.patrouille)
    alias(libs.plugins.maven.publish)
}

group = "nl.bijdorpstudio.kiban"
version = "0.2.0"

compatPatrouille {
    java(libs.versions.java.version.get().toInt())
    kotlin(libs.versions.kotlin.version.get())
}

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    js(IR) {
        nodejs()
        compilerOptions {
            freeCompilerArgs.add("-XXLanguage:+JsAllowInvalidCharsIdentifiersEscaping") // Remove when target Kotlin 2.1+
        }
    }

    sourceSets {
        commonMain.dependencies {
            // no dependencies
        }

        commonTest.dependencies {
            implementation(libs.assertk)
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "nl.bijdorpstudio.kiban"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

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
