# Moving off the standalone binary-compatibility-validator

Issue: [#182](https://github.com/BijdorpStudio/kiban/issues/182)

`apiCheck` is the tooling the 1.0 binary compatibility guarantee rests on
([#150](https://github.com/BijdorpStudio/kiban/issues/150),
[#179](https://github.com/BijdorpStudio/kiban/issues/179)), and the plugin
providing it — `org.jetbrains.kotlinx.binary-compatibility-validator` 0.18.1 — is
in maintenance mode by its own README, with new work going into the ABI
validation built into the Kotlin Gradle plugin instead. #182 asked whether to
follow, and listed five unknowns to resolve before deciding.

**Conclusion: migrated.** All four checklist items came out clean. The dumps
carry over in place, klib coverage is not merely equivalent but byte-identical,
and the one difference in the JVM dump is a synthetic member that no consumer
can reference. What follows is the evidence, including the one thing that could
not be exercised on the runners this project has.

Verified against Kotlin 2.4.10 / Gradle 9.7.0 on a Linux x86-64 container.

## 1. Why the naive enablement did not compile

#182 recorded that adding `abiValidation { }` inside the existing `kotlin { }`
block failed with:

```
e: library/build.gradle.kts:46:5: Unresolved reference 'abiValidation'.
```

It is not the wrong receiver and not an interaction with the Android plugin. It
is a version conflict on the *subproject's* build-script classpath.

`library/build.gradle.kts` is the only place TestBalloon is applied, and
`de.infix.testBalloon:testBalloon-gradle-plugin:1.1.0-RC` depends on
`org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0`. Every other plugin this build
uses — the Kotlin Multiplatform plugin at the pinned 2.4.10 included — is
declared in the root `build.gradle.kts` with `apply false`, so it lands on the
root classpath, and `:library`'s own classpath resolves to exactly one thing:

```
classpath
+--- de.infix.testBalloon:de.infix.testBalloon.gradle.plugin:1.1.0-RC
|    \--- de.infix.testBalloon:testBalloon-gradle-plugin:1.1.0-RC
|         +--- org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0
```

(`./gradlew :library:buildEnvironment` — no `-> 2.4.10` anywhere in the tree.)

KGP 2.2.0's `KotlinTopLevelExtension` has no `abiValidation` member, so with the
2.2.0 API jar shadowing 2.4.10 for script compilation the reference cannot
resolve.

Declaring `alias(libs.plugins.testballoon) apply false` in the root
`build.gradle.kts` puts TestBalloon on the root classpath too, where its 2.2.0
loses the ordinary version conflict against the pinned 2.4.10. `abiValidation`
resolves immediately afterwards. This is worth keeping in mind beyond this
issue: a plugin applied only in a subproject brings its own transitive KGP with
it, and that KGP — not the catalog — decides which DSL the subproject's build
script compiles against.

## 2. The DSL moved between 2.2 and 2.4

The shape in the issue is the 2.2 one. On 2.4.10 both properties it uses are
gone, each with a deprecation that says what replaced it:

* `enabled` — *"Property was removed, to enable ABI validation call function
  `abiValidation()`, `abiValidation { … }` or read `abiValidation` property."*
  Calling the block is the switch.
* `klib { enabled = true }` — *"Block 'klib' was removed. Nested property
  'enabled' was removed - ABI dumps always generated for klib-based targets.
  'keepUnsupportedTargets' was moved to the higher level."*

Both surface as compilation errors, not warnings, so there is no way to carry
the 2.2 shape forward silently. That answers unknown 2 as a side effect: klib
ABI is not something to switch on, it is unconditional for klib-based targets.

## 3. Dump location and format carry over unchanged

`updateKotlinAbi` writes to `library/api/jvm/library.api` and
`library/api/library.klib.api` — the same two paths, in the same layout and the
same format, that the standalone plugin used. No new files, no directory to
migrate, no `.gitignore` change.

Against the dumps committed for 0.6.0:

* **`library.klib.api` is byte-identical.** Same header, same
  `// Targets: [iosArm64, iosSimulatorArm64, iosX64, js, linuxArm64, linuxX64,
  macosArm64, mingwX64, tvosArm64, tvosSimulatorArm64, wasmJs, watchosArm64,
  watchosDeviceArm64, watchosSimulatorArm64]` line, same signature version, same
  declarations. Every published Native, JS and Wasm target is covered exactly as
  before.
* **`library/api/jvm/library.api` differs by one line**, dropped by the built-in
  tool:

  ```
  -	public synthetic fun <init> (Ljava/lang/String;Lkotlin/jvm/internal/DefaultConstructorMarker;)V
  ```

  That is the synthetic bridge constructor Kotlin emits for `Iban`'s private
  constructor. It is `ACC_SYNTHETIC`: no Kotlin or Java source can name it, and
  it exists only so that `Iban.Companion` can reach the private constructor. The
  standalone plugin lists public synthetic constructors; the built-in tool
  filters them. Nothing a consumer can write changes behaviour because of it.

The two tools genuinely disagree on that line rather than merely rendering it
differently — running the old `jvmApiCheck` against the new dump fails on
exactly that one addition, and `checkKotlinAbi` against the old dump fails on
exactly that one removal. So there is no configuration that keeps both green,
which is why this is a migration and not an addition, and why the API dump is
touched in the same commit.

Neither dump has ever carried an `api/android/` directory, and the built-in tool
does not add one, so the Android question in #182 is a non-question here: the
Android target's ABI was not separately dumped before and is not now. The JVM
dump is what covers the JVM/Android surface, as
[VERSIONING.md](../VERSIONING.md) already says.

## 4. `keepLocallyUnsupportedTargets` is set, and was not exercised

This is the one item that could not be tested, and the reason is more
interesting than the item.

The premise in #182 — and in `CLAUDE.md` before this change — was that a Linux
runner cannot build the Apple targets, so the klib check has to infer them.
That is not what happens. `compileKotlinIosArm64`, `compileKotlinMacosArm64`,
`compileKotlinTvosArm64`, `compileKotlinWatchosArm64` and their siblings all run
to completion on a Linux container: producing a *klib* needs no Xcode, only
linking a framework or an executable does. That is why `apiCheck` passed here
all along, and why the byte-identical klib dump above was produced from real
compilations rather than from inference.

Consequently nothing in this project is a "locally unsupported target" on
either of the hosts it is checked on. Dumping with
`keepLocallyUnsupportedTargets` set to `false` and to `true` produces identical
files. It is set to `true` regardless, as the counterpart of
`kotlin.native.ignoreDisabledTargets` in `gradle.properties`: it costs nothing,
and it is what would keep a check on one host agreeing with a check on another
if a future target ever did need a toolchain a runner lacks.

## 5. Experimental on both sides

The replacement needs `@OptIn(ExperimentalAbiValidation::class)`; the klib half
of what it replaces needed `@OptIn(ExperimentalBCVApi::class)`. This is not a
move from experimental to stable, and section 2 above is evidence that the
experimental DSL does move between Kotlin releases. Two things bound the risk:

* The breakage is compile-time and self-describing. A Kotlin bump that reshapes
  the DSL fails the build with a message naming the replacement, in a
  Dependabot PR, before it can reach a release.
* The dumps are the artifact that matters, and they are plain files in the
  repository. If the built-in validation has to be abandoned, the standalone
  plugin reads the same files from the same paths — the only thing to restore
  is the one synthetic-constructor line.

## What this changes

* Root `build.gradle.kts`: the `kotlinx-api-validator` plugin and the
  `apiValidation { }` block are gone; `libs.plugins.testballoon` is declared
  with `apply false` for the classpath reason in section 1. The block's
  `ignoredProjects.add("jvm-cli")` needs no replacement — the built-in
  validation is configured per project, and `:samples:jvm-cli` does not
  configure it.
* `library/build.gradle.kts`: `kotlin { abiValidation { … } }`.
* `gradle/libs.versions.toml`: the
  `org.jetbrains.kotlinx:binary-compatibility-validator` entry is dropped. One
  fewer third-party plugin on the build classpath.
* `apiCheck` → `checkKotlinAbi` and `apiDump` → `updateKotlinAbi` in CI, in
  `CONTRIBUTING.md`, `VERSIONING.md`, `RELEASING.md`, `README.md` and
  `CLAUDE.md`.
