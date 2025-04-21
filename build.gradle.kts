import nl.littlerobots.vcu.plugin.resolver.VersionSelectors.Companion.PREFER_STABLE

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.versions.catalogue.update)
}

versionCatalogUpdate {
    sortByKey = true
    versionSelector(PREFER_STABLE)
}
