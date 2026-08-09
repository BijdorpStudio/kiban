import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compat.tapmoc)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktfmt)
}

group = "nl.bijdorpstudio.kiban"

version = "0.5.0"

ktfmt { kotlinLangStyle() }

dokka {
    moduleName.set("kiban")
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(rootDir)
            remoteUrl("https://github.com/BijdorpStudio/kiban/tree/main")
            remoteLineSuffix.set("#L")
        }
    }
}

tapmoc {
    java(libs.versions.java.version.get().toInt())
    kotlin(libs.versions.kotlin.version.get())
}

kotlin {
    jvm()
    android {
        namespace = "nl.bijdorpstudio.kiban"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {}
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    val kibanFramework = XCFramework("Kiban")
    macosArm64 {
        binaries.framework {
            baseName = "Kiban"
            kibanFramework.add(this)
        }
    }
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    linuxX64()
    linuxArm64()
    mingwX64()
    js {
        nodejs()
        browser { testTask { useKarma { useChromeHeadless() } } }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser { testTask { useKarma { useChromeHeadless() } } }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.assertk)
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "kiban", version.toString())

    // Without this, auto-detection ships the whole Dokka HTML site as -javadoc.jar (see #78).
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = SourcesJar.Sources(),
        )
    )

    pom {
        name = "Kiban"
        description = "Kotlin Multiplatform IBAN Library."
        inceptionYear = "2025"
        url = "https://github.com/BijdorpStudio/kiban"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "barend"
                name = "Barend Garvelink"
                url = "https://github.com/barend"
            }
            developer {
                id = "emartynov"
                name = "Eugen Martynov"
                url = "https://github.com/emartynov"
            }
            developer {
                id = "bijdorpstudio"
                name = "Bijdorp Studio"
                url = "https://bijdorpstudio.nl"
            }
        }
        scm {
            url = "https://github.com/BijdorpStudio/kiban"
            connection = "scm:git:git://github.com/BijdorpStudio/kiban.git"
            developerConnection = "scm:git:ssh://git@github.com:BijdorpStudio/kiban.git"
        }
    }
}

// Guards against a publishing that silently ships a partial artifact set: if a declared
// Kotlin target's toolchain is missing on the publishing host, kotlin.native.ignoreDisabledTargets
// (needed for local dev and PR CI, where no single host can build every target) would otherwise
// skip it without failing the build. This diffs the declared targets against the publications
// the maven-publish plugin actually registered and fails before any upload happens.
val verifyPublicationTargets =
    tasks.register("verifyPublicationTargets") {
        group = "verification"
        description = "Fails if declared Kotlin targets and registered Maven publications diverge."
        doLast {
            val expectedPublications =
                kotlin.targets
                    .map { target ->
                        if (target.name == "metadata") "kotlinMultiplatform" else target.name
                    }
                    .toSortedSet()
            val actualPublications = publishing.publications.names.toSortedSet()
            check(expectedPublications == actualPublications) {
                val missing = expectedPublications - actualPublications
                val unexpected = actualPublications - expectedPublications
                buildString {
                    appendLine("Declared Kotlin targets and Maven publications are out of sync.")
                    if (missing.isNotEmpty())
                        appendLine(
                            "Targets with no publication (likely skipped by kotlin.native.ignoreDisabledTargets): $missing"
                        )
                    if (unexpected.isNotEmpty())
                        appendLine("Publications with no matching declared target: $unexpected")
                }
            }
        }
    }

tasks.withType<PublishToMavenRepository>().configureEach { dependsOn(verifyPublicationTargets) }
