import nl.littlerobots.vcu.plugin.resolver.VersionSelectors.Companion.PREFER_STABLE

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.versions.catalogue.update)
    alias(libs.plugins.kotlinx.api.validator)
    alias(libs.plugins.compat.tapmoc) apply false
}

@OptIn(kotlinx.validation.ExperimentalBCVApi::class)
apiValidation {
    klib {
        enabled = true
    }
}

versionCatalogUpdate {
    sortByKey = true
    versionSelector(PREFER_STABLE)
}
