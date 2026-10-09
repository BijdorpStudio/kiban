import nl.littlerobots.vcu.plugin.resolver.VersionSelectors.Companion.PREFER_STABLE

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.versions.catalogue.update)
    alias(libs.plugins.compat.tapmoc) apply false
    // Declared here, and not only in ':kiban' where it is applied, so that its transitive Kotlin
    // Gradle plugin loses the version conflict against the one this build pins. See
    // docs/209-build-script-notes.md.
    alias(libs.plugins.testballoon) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.ktfmt)
}

ktfmt { kotlinLangStyle() }

versionCatalogUpdate {
    sortByKey = true
    versionSelector(PREFER_STABLE)
}
