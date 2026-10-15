import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktfmt)
}

ktfmt { kotlinLangStyle() }

// Read rather than hardcoded, so the probe cannot drift off the version being published.
// 'publish.yml' reads the same property out of the same file. This is a build of its own, so the
// library's 'gradle.properties' is not on this build's property lookup path and has to be loaded
// by hand (#215).
val kibanVersion: String =
    rootDir
        .resolve("../../gradle.properties")
        .inputStream()
        .use { Properties().apply { load(it) } }
        .getProperty("VERSION_NAME")
        ?: error("Could not read 'VERSION_NAME' from gradle.properties.")

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
