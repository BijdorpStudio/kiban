import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import java.io.File
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

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

    coordinates(group.toString(), "kiban", version.toString())

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

// Guards against a publish that silently ships a partial artifact set, by diffing the declared
// Kotlin targets against the registered Maven publications. See docs/209-build-script-notes.md.
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

// KGP registers no test task for linuxArm64, so this runs the cross-compiled binary under the
// qemu-aarch64 emulator the Kotlin/Native toolchain ships. Every detail of this block, and why
// each guard is there, is in docs/209-build-script-notes.md - read that before changing it.
//
// Guarded on the link task's existence: 'kotlin.native.ignoreDisabledTargets' drops targets the
// host cannot build, and this must not fail configuration for every other task there.
if ("linkDebugTestLinuxArm64" in tasks.names) {

    val linuxArm64TestLink = tasks.named<KotlinNativeLink>("linkDebugTestLinuxArm64")

    tasks.register<KotlinNativeHostTest>("linuxArm64Test") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description =
            "Executes Kotlin/Native unit tests for target linuxArm64 under the qemu-aarch64 emulator."
        targetName = "linuxArm64"
        workingDir = projectDir.absolutePath

        // linux_x64 is the only host konan.properties declares an emulator for; elsewhere the
        // task is disabled rather than failing.
        enabled = HostManager.hostOrNull == KonanTarget.LINUX_X64

        // Set explicitly because the helper applying KGP's own convention is internal.
        binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/$name/binary"))
        reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/$name"))
        reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/$name"))

        val testBinary = linuxArm64TestLink.flatMap { it.outputFile }
        dependsOn(linuxArm64TestLink)
        // The binary reaches the emulator as an argument, which Gradle does not track by itself.
        // Declaring it keeps a rebuilt binary from being reported as up to date.
        inputs.file(testBinary).withPropertyName("testBinary")

        // Resolved while the task runs, because the link task above is what downloads them. The
        // lookup is local so the lambda captures nothing and the provider stays
        // configuration-cacheable.
        val emulatorAndSysroot =
            providers
                .environmentVariable("KONAN_DATA_DIR")
                .orElse(providers.systemProperty("user.home").map { "$it/.konan" })
                .map { konanDataDir ->
                    val dependencies = File(konanDataDir, "dependencies")
                    fun dependency(prefix: String, path: String): File {
                        // The highest version wins: 'listFiles' is in filesystem order and a
                        // Kotlin upgrade can leave the previous version's directory behind.
                        val root =
                            dependencies
                                .listFiles()
                                .orEmpty()
                                .filter { it.name.startsWith(prefix) }
                                .maxByOrNull { it.name }
                                ?: error("No Kotlin/Native dependency '$prefix*' in $dependencies.")
                        // 'executable' is '@SkipWhenEmpty', so a missing path would skip the
                        // task silently instead of failing it.
                        return root.resolve(path).also {
                            check(it.exists()) { "Kotlin/Native dependency has no $it." }
                        }
                    }
                    dependency("qemu-aarch64-static-", "qemu-aarch64") to
                        dependency(
                            "aarch64-unknown-linux-gnu-",
                            "aarch64-unknown-linux-gnu/sysroot",
                        )
                }

        executable(emulatorAndSysroot.map { (emulator, _) -> emulator })
        // 'args' takes no provider, so it is set once the link task has run. Everything after the
        // binary is passed through to it, which is where KGP appends its own arguments.
        doFirst {
            val (_, sysroot) = emulatorAndSysroot.get()
            args = listOf("-L", sysroot.absolutePath, testBinary.get().absolutePath)
        }
    }
}
