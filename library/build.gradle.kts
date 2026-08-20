import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import java.io.File
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
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

// linuxArm64 is published but has never run a test. Kotlin/Native has no Linux/ARM64 *host*
// compiler, so no machine can both build and run the target: an ARM64 runner cannot build it at
// all, and on the linux-x86_64 host that cross-compiles it the Kotlin Gradle plugin registers no
// test task, because that host cannot execute what it produces (#151).
//
// The toolchain that cross-compiles the target ships what it takes to run it anyway.
// konan.properties declares a qemu-aarch64 user-mode emulator for the linux_x64-linux_arm64 pair,
// and the aarch64 sysroot the binary is linked against comes down with the same toolchain, so
// emulator, sysroot and binary always match. Pointing a KotlinNativeHostTest at the emulator gives
// the target the same treatment as every other one: Gradle reads the results out of the binary's
// TeamCity service messages, writes the JUnit XML that CI reports from, and fails the build on a
// failing test. The binary's own exit code says nothing - KGP invokes it with
// '--ktest_no_exit_code', and it exits 0 whatever the tests did.
//
// Guarded on the link task's existence because 'kotlin.native.ignoreDisabledTargets' (set in
// gradle.properties) drops targets the host cannot build: where there is nothing to link there is
// nothing to run, and this must not fail configuration for every other task on that host.
if ("linkDebugTestLinuxArm64" in tasks.names) {

    val linuxArm64TestLink = tasks.named<KotlinNativeLink>("linkDebugTestLinuxArm64")

    tasks.register<KotlinNativeHostTest>("linuxArm64Test") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description =
            "Executes Kotlin/Native unit tests for target linuxArm64 under the qemu-aarch64 emulator."
        targetName = "linuxArm64"
        workingDir = projectDir.absolutePath

        // linux_x64 is the only host konan.properties declares an emulator for. Elsewhere the
        // task is disabled rather than failing, which is how KGP treats a test task it cannot run.
        enabled = HostManager.hostOrNull == KonanTarget.LINUX_X64

        // KGP puts these under the task name; they are set explicitly because the helper that
        // applies that convention lives in an internal package.
        binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/$name/binary"))
        reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/$name"))
        reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/$name"))

        val testBinary = linuxArm64TestLink.flatMap { it.outputFile }
        dependsOn(linuxArm64TestLink)
        // The binary reaches the emulator as an argument, which Gradle does not track by itself.
        // Declaring it keeps a rebuilt binary from being reported as up to date.
        inputs.file(testBinary).withPropertyName("testBinary")

        // The emulator and the sysroot it needs to load an aarch64 binary both live under the
        // konan dependencies directory, in a directory whose name carries the dependency's
        // version. Resolved while the task runs rather than while the build is configured: the
        // link task above is what downloads them, so on a cold machine neither exists yet.
        //
        // The lookup is a local function rather than a shared one so that the lambda captures
        // nothing from the build script, which is what keeps the provider configuration-cacheable.
        val emulatorAndSysroot =
            providers
                .environmentVariable("KONAN_DATA_DIR")
                .orElse(providers.systemProperty("user.home").map { "$it/.konan" })
                .map { konanDataDir ->
                    val dependencies = File(konanDataDir, "dependencies")
                    fun dependency(prefix: String, path: String): File {
                        // 'listFiles' is in filesystem order, and a Kotlin upgrade can leave the
                        // previous version's directory behind, so the highest name wins rather
                        // than whichever one is listed last.
                        val root =
                            dependencies
                                .listFiles()
                                .orEmpty()
                                .filter { it.name.startsWith(prefix) }
                                .maxByOrNull { it.name }
                                ?: error("No Kotlin/Native dependency '$prefix*' in $dependencies.")
                        // Checked because 'executable' is '@SkipWhenEmpty': a path that does not
                        // exist would skip the task silently instead of failing it.
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
        // 'args' takes no provider, so it is set once the link task has run. The emulator passes
        // everything after the binary through to it, which is where KGP appends its own
        // '--ktest_logger=TEAMCITY' and friends: the arguments set here come first.
        doFirst {
            val (_, sysroot) = emulatorAndSysroot.get()
            args = listOf("-L", sysroot.absolutePath, testBinary.get().absolutePath)
        }
    }
}
