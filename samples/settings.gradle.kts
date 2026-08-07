// Deliberately a standalone Gradle build, not a subproject of the root build (see #68): it must
// resolve kiban by Maven coordinates against the artifact CI just published, not by Gradle
// project substitution against :library. Invoke with `../gradlew -p samples <task>` from here, or
// `./gradlew -p samples <task>` from the repo root.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "kiban-samples"

include(":jvm-cli")
