import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":library"))
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
