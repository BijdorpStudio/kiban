import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

group = "nl.bijdorpstudio.kiban"
version = "0.2.0"

val javaVersion = JavaVersion.VERSION_17
val jvmTargetVersion = JvmTarget.JVM_17
val kotlinVersion = KotlinVersion.KOTLIN_1_9

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(jvmTargetVersion)
            freeCompilerArgs.add("-Xjdk-release=${jvmTargetVersion.target}")
        }
    }
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(jvmTargetVersion)
        }
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

    compilerOptions {
        apiVersion.set(kotlinVersion)
        languageVersion.set(kotlinVersion)
    }

    coreLibrariesVersion = "${kotlinVersion.version}.0" // Kotlin versions have only major and minor without patch

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
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

tasks.withType<JavaCompile>().configureEach {
    if (project.hasProperty("android")) {
        sourceCompatibility = javaVersion.majorVersion
        targetCompatibility = javaVersion.majorVersion
    } else {
        options.release.set(jvmTargetVersion.target.toInt())
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
