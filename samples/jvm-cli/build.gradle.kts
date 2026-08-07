plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktfmt)
    application
}

ktfmt { kotlinLangStyle() }

dependencies {
    implementation(project(":library"))
    testImplementation(kotlin("test-junit5"))
}

application { mainClass.set("nl.bijdorpstudio.kiban.samples.jvmcli.MainKt") }

tasks.test { useJUnitPlatform() }
