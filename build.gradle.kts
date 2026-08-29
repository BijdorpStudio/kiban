import nl.littlerobots.vcu.plugin.resolver.VersionSelectors.Companion.PREFER_STABLE

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.versions.catalogue.update)
    alias(libs.plugins.compat.tapmoc) apply false
    // Declared here, and not only in ':library' where it is applied, so that its transitive Kotlin
    // Gradle plugin loses the version conflict against the one this build pins. TestBalloon
    // 1.1.0-RC brings kotlin-gradle-plugin 2.2.0 with it; applied only in the subproject, that
    // 2.2.0 is the sole KGP on the subproject's own script classpath and shadows the pinned
    // version, which is what made 'kotlin { abiValidation { } }' an unresolved reference (#182).
    alias(libs.plugins.testballoon) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.ktfmt)
}

ktfmt { kotlinLangStyle() }

versionCatalogUpdate {
    sortByKey = true
    versionSelector(PREFER_STABLE)
}
