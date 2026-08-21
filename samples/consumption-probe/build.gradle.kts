plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktfmt)
}

ktfmt { kotlinLangStyle() }

// Read rather than hardcoded, so the probe cannot drift off the version being published.
// 'publish.yml' reads the same literal the same way.
val kibanVersion: String =
    Regex("""^version = "(.*)"$""", RegexOption.MULTILINE)
        .find(rootDir.resolve("../../library/build.gradle.kts").readText())
        ?.groupValues
        ?.get(1) ?: error("Could not read 'version' from library/build.gradle.kts.")

kotlin {
    // One target per compilation backend - the axis metadata resolution breaks along. Linux-only
    // because of linuxX64, which is why CI pins this job to a Linux runner.
    jvm()
    linuxX64()
    js { nodejs() }

    sourceSets {
        commonMain.dependencies { implementation("nl.bijdorpstudio.kiban:kiban:$kibanVersion") }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
