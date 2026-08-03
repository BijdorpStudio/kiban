import nl.littlerobots.vcu.plugin.resolver.VersionSelectors.Companion.PREFER_STABLE

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.versions.catalogue.update)
    alias(libs.plugins.kotlinx.api.validator)
    alias(libs.plugins.compat.tapmoc) apply false
    alias(libs.plugins.dokka) apply false
}

@OptIn(kotlinx.validation.ExperimentalBCVApi::class)
apiValidation {
    klib {
        enabled = true
    }
    // kiban-test (see its README) has no public API surface yet, and its baseline dump
    // depends on targets (Android, Kotlin/Native) this environment can't build. Drop it
    // once the first real helper lands and `./gradlew :kiban-test:apiDump` has been run
    // on a host that can build the full target matrix.
    ignoredProjects.add("kiban-test")
}

versionCatalogUpdate {
    sortByKey = true
    versionSelector(PREFER_STABLE)
}
