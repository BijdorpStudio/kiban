plugins {
    kotlin("jvm") version "2.4.10"
    application
    id("com.ncorti.ktfmt.gradle") version "0.27.0"
}

ktfmt { kotlinLangStyle() }

// Tracks library/build.gradle.kts's `version`. Deliberately not the latest Maven Central release
// (see README's "Installation" section): CI publishes this exact version to mavenLocal before
// building this sample, so it always exercises the artifact this PR/commit actually produces.
dependencies {
    implementation("nl.bijdorpstudio.kiban:kiban:0.5.0")
    testImplementation(kotlin("test-junit5"))
}

application { mainClass.set("nl.bijdorpstudio.kiban.samples.jvmcli.MainKt") }

tasks.test { useJUnitPlatform() }
