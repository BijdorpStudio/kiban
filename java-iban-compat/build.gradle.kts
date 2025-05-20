plugins {
    kotlin("jvm") version "1.9.22" // Or your project's Kotlin version
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":library"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(11)
}

// Specify source sets if not automatically inferred, though Gradle usually infers this correctly
// sourceSets {
//     main {
//         kotlin.srcDir("src/main/kotlin")
//     }
//     test {
//         kotlin.srcDir("src/test/kotlin")
//     }
// }
