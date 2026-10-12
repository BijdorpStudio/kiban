import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compat.tapmoc)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.testballoon)
}

group = "nl.bijdorpstudio.kiban"

version = "0.6.0"

ktfmt { kotlinLangStyle() }

// Versioned API docs (#162). Every release overwrites GitHub Pages with the Dokka output of the
// version being published, so without this a consumer still on an older line loses their reference
// the moment the next release goes out. The versioning plugin turns the site into an archive: it
// renders a version dropdown, keeps the version being generated at the root (so the Pages URL
// always lands on the latest docs), and copies the versions handed to it below into 'older/'.
//
// The archive is not in this repository - it is the previously published site, restored by the
// 'docs' job in 'publish.yml' and pointed at with this property. The property is deliberately
// optional: with no value the DirectoryProperty stays unset and Dokka generates exactly the
// single-version site it did before, which is what a local ':kiban:dokkaGeneratePublicationHtml'
// wants and what the very first versioned release has to produce anyway (no archive exists yet).
// See docs/162-versioned-api-docs.md.
val previousDocVersionsDir: Provider<Directory> =
    providers.gradleProperty("kiban.previousDocVersions").map { path ->
        // Absolute in CI; resolved against the project directory otherwise, as Gradle's own
        // file-path properties are.
        layout.projectDirectory.dir(path)
    }

dependencies { dokkaPlugin(libs.dokka.versioning.plugin) }

dokka {
    // No moduleName: Dokka defaults it to the Gradle project name, which is the published
    // artifact name (#208).
    pluginsConfiguration.versioning {
        // The version of the docs being generated, which is the version being released: the tag
        // guard in 'publish.yml' has already checked that these agree.
        version.set(project.version.toString())
        olderVersionsDir.set(previousDocVersionsDir)
    }
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
    // Binary compatibility validation, the tooling the 1.0 guarantee rests on (#150, #179). This is
    // the Kotlin Gradle plugin's own implementation rather than the standalone
    // binary-compatibility-validator, which is in maintenance mode with new work going here
    // instead (#182). Dumps land in 'kiban/api' in the same layout and format the standalone
    // plugin used, so the committed reference files carried over.
    //
    // Since Kotlin 2.4.20 the android target gets its own JVM-class dump,
    // 'kiban/api/android/kiban.api', alongside the jvm one; 2.4.10 and the standalone plugin
    // before it dumped neither (see docs/182-builtin-abi-validation.md). It is byte-identical to
    // the jvm dump and will stay that way while there is no 'androidMain' source set: both
    // targets compile commonMain alone, and jvmMain holds only the 'IBAN' typealias, which is
    // erased and so reaches no dump. The duplication is the tool's, not a choice made here -
    // suppressing it would drop the android surface from validation instead of deduplicating it.
    //
    // The consequence for anyone running this task: 'checkKotlinAbi' now needs an Android SDK,
    // because dumping that target compiles androidMain. See CLAUDE.md.
    //
    // Nothing to switch on: calling the block is what enables validation - the 'enabled' property
    // it took in Kotlin 2.2 is gone, as is the 'klib { enabled }' that turned klib dumping on,
    // because klib-based targets are now always dumped.
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        // Keeps a target the publishing host cannot build in the dump instead of dropping it,
        // which is what makes a check on one host agree with a check on another - the counterpart
        // of 'kotlin.native.ignoreDisabledTargets' in gradle.properties. Not exercised by a Linux
        // container or by CI's runners: every klib target here compiles on both, Apple ones
        // included, since a klib needs no Xcode. It is set for the host that cannot.
        keepLocallyUnsupportedTargets.set(true)
    }

    // Every public declaration must state its visibility and return type deliberately, so nothing
    // reaches the frozen API surface by omission. Applies to production source sets only; test
    // sources are exempt.
    explicitApi()

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
            implementation(libs.testballoon.framework.core)
        }
        // TestBalloon runs Android host-side tests through the JUnit 4 runner, which the Android
        // target does not put on the test classpath by itself.
        named("androidHostTest").dependencies { implementation(libs.junit) }
    }
}

// Off only for the mavenLocal consumption probe (samples/consumption-probe), which has no signing
// key: applied, every local publish fails on "No configured signatory", and skipping the Sign tasks
// with '-x' fails on the missing '.asc' files instead. 'publish.yml' never sets this, so a release
// still signs or fails.
val signPublications: Boolean =
    providers.gradleProperty("kiban.signPublications").map(String::toBoolean).getOrElse(true)

mavenPublishing {
    publishToMavenCentral()

    if (signPublications) {
        signAllPublications()
    }

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
