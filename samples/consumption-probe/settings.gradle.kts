// A build of its own on purpose. Included in the root build - or wired up with 'includeBuild' -
// dependency substitution would replace the coordinates below with ':library', and the probe would
// pass without reading a published file.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // Exclusive, not merely first: the version under development is usually one Maven Central
        // already holds, so a probe able to fall back would keep passing against the last release
        // when the local publish produced nothing.
        exclusiveContent {
            forRepository { mavenLocal() }
            filter { includeGroup("nl.bijdorpstudio.kiban") }
        }
        mavenCentral()
    }

    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}

rootProject.name = "consumption-probe"
