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

// The publication coordinates come from 'GROUP' and 'VERSION_NAME' in the root
// 'gradle.properties', which the maven-publish plugin reads natively. These assignments are not
// what publishes them: the plugin leaves 'project.group' and 'project.version' alone, and the klib
// unique name and the Dokka version below are derived from those.
group = providers.gradleProperty("GROUP").get()

version = providers.gradleProperty("VERSION_NAME").get()

ktfmt { kotlinLangStyle() }

// The previously published Dokka site, which the versioning plugin puts behind a version
// dropdown. Deliberately optional: unset, Dokka generates a plain single-version site. See
// docs/209-build-script-notes.md and docs/162-versioned-api-docs.md.
val previousDocVersionsDir: Provider<Directory> =
    providers.gradleProperty("kiban.previousDocVersions").map { path ->
        // Absolute in CI; resolved against the project directory otherwise, as Gradle's own
        // file-path properties are.
        layout.projectDirectory.dir(path)
    }

dependencies { dokkaPlugin(libs.dokka.versioning.plugin) }

dokka {
    // No moduleName: Dokka defaults it to the Gradle project name, which is the published
    // artifact name.
    pluginsConfiguration.versioning {
        // The version being released; 'publish.yml' has already checked the tag agrees.
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
    // Binary compatibility validation, the tooling the 1.0 guarantee rests on. Calling the block
    // is what enables it; 'checkKotlinAbi' needs an Android SDK (see CLAUDE.md). See
    // docs/209-build-script-notes.md and docs/182-builtin-abi-validation.md.
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        // Keeps a target the publishing host cannot build in the dump instead of dropping it, so a
        // check on one host agrees with a check on another.
        keepLocallyUnsupportedTargets.set(true)
    }

    // Nothing may reach the frozen API surface by omission; production source sets only.
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
        // target does not put on the test classpath itself.
        named("androidHostTest").dependencies { implementation(libs.junit) }
    }
}

// Off only for the mavenLocal consumption probe (samples/consumption-probe), which has no signing
// key. 'publish.yml' never sets this, so a release still signs or fails. See
// docs/209-build-script-notes.md.
val signPublications: Boolean =
    providers.gradleProperty("kiban.signPublications").map(String::toBoolean).getOrElse(true)

mavenPublishing {
    publishToMavenCentral()

    if (signPublications) {
        signAllPublications()
    }

    // Without this, auto-detection ships the whole Dokka HTML site as -javadoc.jar.
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
