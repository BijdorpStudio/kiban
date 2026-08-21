// A build of its own, deliberately not included from the root 'settings.gradle.kts'.
//
// The point of this probe is to resolve kiban the way a consumer does - by coordinates, out of a
// repository, through the Gradle module metadata the publish flow produced. Including it in the
// root build (or wiring it up with 'includeBuild') would defeat that: Gradle's dependency
// substitution would quietly replace 'nl.bijdorpstudio.kiban:kiban:<version>' with ':library'
// again, and the probe would pass without a single published file being read.
//
// It is driven from the root wrapper, so it needs no wrapper of its own. See samples/README.md for
// what the two commands do and why they look like this:
//
//   ./gradlew -Pkiban.signPublications=false \
//     :library:publishKotlinMultiplatformPublicationToMavenLocal \
//     :library:publishJvmPublicationToMavenLocal \
//     :library:publishLinuxX64PublicationToMavenLocal \
//     :library:publishJsPublicationToMavenLocal
//   ./gradlew -p samples/consumption-probe check
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // kiban comes out of 'mavenLocal()' and nowhere else. Ordinary repository order would not
        // be enough: the version under development is usually one Maven Central already holds
        // (0.6.0 is released while 'library/build.gradle.kts' still declares it), so a probe that
        // could fall back to Central would keep passing against the *last released* artifact if the
        // local publish were skipped or produced nothing. 'exclusiveContent' turns that silent pass
        // into an unresolvable dependency, which is the only useful outcome here.
        exclusiveContent {
            forRepository { mavenLocal() }
            filter { includeGroup("nl.bijdorpstudio.kiban") }
        }
        // Everything else - the Kotlin stdlib, kotlin-test, the compiler's own dependencies.
        mavenCentral()
    }

    // Shared with the main build rather than copied, so the probe compiles against the same Kotlin
    // version the library was published with. A consumer on a different version is a separate
    // question (tapmoc's compatibility metadata covers it); this one is about metadata resolution.
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}

rootProject.name = "consumption-probe"
