plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktfmt)
}

ktfmt { kotlinLangStyle() }

// Read out of the library's build script rather than hardcoded, so the probe cannot drift into
// resolving a version that is no longer the one being published. 'publish.yml' reads the same
// literal the same way to check a release tag against it, and RELEASING.md names that assignment as
// the one place a version is bumped.
val kibanVersion: String =
    Regex("""^version = "(.*)"$""", RegexOption.MULTILINE)
        .find(rootDir.resolve("../../library/build.gradle.kts").readText())
        ?.groupValues
        ?.get(1) ?: error("Could not read 'version' from library/build.gradle.kts.")

kotlin {
    // Three of the library's targets, one per compilation backend, which is the axis metadata
    // resolution can break along: a variant missing from the root module's Gradle module metadata,
    // or an attribute that no longer matches, fails here as an unresolvable dependency. Covering
    // every published target would multiply build time for a check that is about the metadata, not
    // about the targets themselves.
    jvm()
    // Kotlin/Native. Not built on hosts that cannot target it - see gradle.properties - which is
    // why CI pins this probe to a Linux runner.
    linuxX64()
    js { nodejs() }

    sourceSets {
        commonMain.dependencies {
            // By coordinates, out of 'mavenLocal()'. The whole probe exists for this line.
            implementation("nl.bijdorpstudio.kiban:kiban:$kibanVersion")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
