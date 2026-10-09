# Build script and version catalog notes

Issue: [#209](https://github.com/BijdorpStudio/kiban/issues/209)

Why `kiban/build.gradle.kts` and `gradle/libs.versions.toml` are configured the way they are. The
CI workflows that drive these tasks have their own page,
[209-ci-workflow-notes.md](209-ci-workflow-notes.md). What a contributor has to *run* is in
[CONTRIBUTING.md](../CONTRIBUTING.md); what a sandbox can run is in [CLAUDE.md](../CLAUDE.md).

## `explicitApi()`

Every public declaration must state its visibility and return type deliberately, so nothing reaches
the frozen API surface by omission. It applies to production source sets only; test sources are
exempt.

## `abiValidation { }`

Binary compatibility validation is the tooling the 1.0 guarantee rests on. This is the Kotlin Gradle
plugin's own implementation rather than the standalone `binary-compatibility-validator`, which is in
maintenance mode with new work going here instead — see
[182-builtin-abi-validation.md](182-builtin-abi-validation.md). Dumps land in `kiban/api` in the
same layout and format the standalone plugin used, so the committed reference files carried over.

There is nothing to switch on: calling the block is what enables validation. The `enabled` property
it took in Kotlin 2.2 is gone, as is the `klib { enabled }` that turned klib dumping on, because
klib-based targets are now always dumped.

`keepLocallyUnsupportedTargets.set(true)` keeps a target the publishing host cannot build in the
dump instead of dropping it, which is what makes a check on one host agree with a check on another —
the counterpart of `kotlin.native.ignoreDisabledTargets` in `gradle.properties`. It is not exercised
by a Linux container or by CI's runners: every klib target here compiles on both, Apple ones
included, since a klib needs no Xcode. It is set for the host that cannot.

### The android dump duplicates the jvm one

Since Kotlin 2.4.20 the android target gets its own JVM-class dump, `kiban/api/android/kiban.api`,
alongside the jvm one; 2.4.10 and the standalone plugin before it dumped neither. It is
byte-identical to the jvm dump and will stay that way while there is no `androidMain` source set:
both targets compile `commonMain` alone, and `jvmMain` holds only the `IBAN` typealias, which is
erased and so reaches no dump. The duplication is the tool's, not a choice made here — suppressing
it would drop the android surface from validation instead of deduplicating it.

The consequence for anyone running the task: `checkKotlinAbi` needs an Android SDK, because dumping
that target compiles `androidMain`. See [CLAUDE.md](../CLAUDE.md) for the install.

## Versioned API docs

Every release overwrites GitHub Pages with the Dokka output of the version being published, so
without the versioning plugin a consumer still on an older line loses their reference the moment the
next release goes out. The plugin turns the site into an archive: it renders a version dropdown,
keeps the version being generated at the root (so the Pages URL always lands on the latest docs),
and copies the versions handed to it into `older/`. See
[162-versioned-api-docs.md](162-versioned-api-docs.md).

The archive is not in this repository — it is the previously published site, restored by the `docs`
job in `publish.yml` and pointed at with the `kiban.previousDocVersions` property. That property is
deliberately optional: with no value the `DirectoryProperty` stays unset and Dokka generates exactly
the single-version site it did before, which is what a local
`:kiban:dokkaGeneratePublicationHtml` wants and what the very first versioned release has to produce
anyway, no archive existing yet. The path is absolute in CI and resolved against the project
directory otherwise, as Gradle's own file-path properties are.

The version handed to the plugin is `project.version`, which is the version being released: the tag
guard in `publish.yml` has already checked that the two agree.

No `moduleName` is set: Dokka defaults it to the Gradle project name, which is the published
artifact name.

## Publishing

`JavadocJar.Empty()` is passed explicitly because auto-detection otherwise ships the whole Dokka
HTML site as `-javadoc.jar`.

`kiban.signPublications` exists only for the `mavenLocal` consumption probe
(`samples/consumption-probe`), which has no signing key. With signing applied, every local publish
fails on "No configured signatory", and skipping the `Sign` tasks with `-x` fails on the missing
`.asc` files instead. `publish.yml` never sets the property, so a release still signs or fails.

`verifyPublicationTargets` guards against a publish that silently ships a partial artifact set. If a
declared Kotlin target's toolchain is missing on the publishing host, `kotlin.native.ignoreDisabledTargets`
— needed for local dev and PR CI, where no single host can build every target — would otherwise skip
it without failing the build. The task diffs the declared targets against the publications the
`maven-publish` plugin actually registered, and fails before any upload happens.

## `linuxArm64Test` runs under the qemu emulator

`linuxArm64` is published but has never run a test. Kotlin/Native has no Linux/ARM64 *host*
compiler, so no machine can both build and run the target: an ARM64 runner cannot build it at all,
and on the linux-x86_64 host that cross-compiles it the Kotlin Gradle plugin registers no test task,
because that host cannot execute what it produces.

The toolchain that cross-compiles the target ships what it takes to run it anyway.
`konan.properties` declares a qemu-aarch64 user-mode emulator for the `linux_x64`-`linux_arm64`
pair, and the aarch64 sysroot the binary is linked against comes down with the same toolchain, so
emulator, sysroot and binary always match. Pointing a `KotlinNativeHostTest` at the emulator gives
the target the same treatment as every other one: Gradle reads the results out of the binary's
TeamCity service messages, writes the JUnit XML that CI reports from, and fails the build on a
failing test. The binary's own exit code says nothing — KGP invokes it with `--ktest_no_exit_code`,
and it exits 0 whatever the tests did.

The details that are easy to get wrong when touching this block:

* **Registration is guarded on the link task's existence.** `kotlin.native.ignoreDisabledTargets`
  drops targets the host cannot build; where there is nothing to link there is nothing to run, and
  this must not fail configuration for every other task on that host.
* **`enabled` is guarded on the host.** `linux_x64` is the only host `konan.properties` declares an
  emulator for. Elsewhere the task is disabled rather than failing, which is how KGP treats a test
  task it cannot run.
* **The report locations are set explicitly.** KGP puts these under the task name, but the helper
  that applies that convention lives in an internal package.
* **The test binary is declared as an input.** It reaches the emulator as an argument, which Gradle
  does not track by itself; declaring it keeps a rebuilt binary from being reported as up to date.
* **The emulator and sysroot are resolved while the task runs**, not while the build is configured:
  the link task is what downloads them, so on a cold machine neither exists yet. Their directory
  names carry the dependency's version, and a Kotlin upgrade can leave the previous version's
  directory behind, so the highest name wins rather than whichever one `listFiles` returns last.
  Existence is checked because `executable` is `@SkipWhenEmpty`: a path that does not exist would
  skip the task silently instead of failing it. The lookup is a local function rather than a shared
  one so that the lambda captures nothing from the build script, which is what keeps the provider
  configuration-cacheable.
* **`args` is set in `doFirst`**, because it takes no provider. The emulator passes everything after
  the binary through to it, which is where KGP appends its own `--ktest_logger=TEAMCITY` and
  friends: the arguments set in `doFirst` come first.

#218 asks whether to keep this at all or move it into a tested build-logic module; until that is
settled, the block above is the thing to read before changing it.

## `kotlin-version`: the consumer Kotlin floor

`kotlin-version` in `gradle/libs.versions.toml` is the minimum Kotlin version a *consumer* needs,
not merely what the library is built with (that is `kotlin`). Raising it is a breaking change for
consumers who cannot follow, so after 1.0 it only moves in a major release — see
[VERSIONING.md](../VERSIONING.md). That is why it moved to 2.4.0 before the freeze rather than after
it.

Two things depend on the floor:

* `@IntroducedAt` / `ExperimentalVersionOverloading` (the `kotlin` package) do not resolve below a
  2.4 `languageVersion`, and they are what lets an optional parameter be added to an already
  published function without breaking binary compatibility.
* `kotlin.time.Instant` (`CountryCodes.lastUpdateDate`) is only a non-experimental stdlib
  declaration from 2.3, so any floor below that turns a frozen public API into one that demands
  `@OptIn` from callers. It is subsumed by the 2.4 floor, but it is the reason the floor can never go
  below 2.3. See [144-instant-api-stability.md](144-instant-api-stability.md).

## The TestBalloon plugin is declared in the root build script

`alias(libs.plugins.testballoon) apply false` sits in the root `build.gradle.kts`, not only in
`:kiban` where the plugin is actually applied, so that its transitive Kotlin Gradle plugin loses the
version conflict against the one this build pins. TestBalloon 1.1.0 brings `kotlin-gradle-plugin`
2.2.0 with it; applied only in the subproject, that 2.2.0 is the sole KGP on the subproject's own
script classpath and shadows the pinned version — which is what made `kotlin { abiValidation { } }`
an unresolved reference. See
[182-builtin-abi-validation.md](182-builtin-abi-validation.md) for how that was diagnosed.

## `testballoon`

Test-only. The 1.1.0 line is the first unified release: one artifact set auto-adapting to the Kotlin
compiler in use (2.2.0+), instead of 1.0.x's per-Kotlin-version variants, which would have made
every Kotlin bump a coupled TestBalloon bump. See
[82-testballoon-evaluation.md](82-testballoon-evaluation.md).

Keep it on a stable release: the verification suite it gates is what gates `publish.yml`, so a
release candidate or beta here puts a prerelease in front of every publish.

## `dokka-versioning-plugin`

A Dokka plugin artifact, not a Gradle plugin: it is added to the `dokkaPlugin` configuration so the
Dokka generator itself loads it. Version-locked to the Dokka Gradle plugin.

## The android host test needs JUnit 4 on the classpath

TestBalloon runs Android host-side tests through the JUnit 4 runner, which the Android target does
not put on the test classpath by itself — hence the explicit `junit` dependency on `androidHostTest`.
