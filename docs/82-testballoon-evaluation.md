# #82 spike: migrate the test suite from `kotlin.test` to TestBalloon?

This is the evaluation #82 asked for: decide whether to move kiban's test suite off
`kotlin.test` onto [TestBalloon](https://github.com/infix-de/testBalloon), and scope the
migration if the answer is yes.

**Recommendation: yes, migrate — but land it once TestBalloon 1.1.0 is released, not now.**
TestBalloon works on this repo's exact toolchain today, covers every one of kiban's 16 declared
targets, and leaves the published API untouched. The one real ongoing cost is that 1.0.x ships
a *separate artifact set per Kotlin version*, so every Kotlin bump would become a coupled
TestBalloon bump. The unreleased 1.1.0 removes exactly that coupling. Waiting for it costs
nothing — the current suite is not blocking anyone — and removes the only durable objection.

Everything below was measured against this repo's actual toolchain (Kotlin 2.4.10, Gradle
9.6.1, JDK 17 toolchain) by applying TestBalloon to `:library` and running real tests, not
inferred from documentation. The probe files were deleted before committing; the observations
they produced are quoted verbatim.

## How the probe was set up

TestBalloon `1.0.1-K2.4.0` was applied to `:library` — the Gradle plugin
`id("de.infix.testBalloon")` plus
`implementation("de.infix.testBalloon:testBalloon-framework-core:1.0.1-K2.4.0")` in
`commonTest` — alongside the existing `kotlin.test` + `assertk` dependencies, which were left
in place. Four throwaway suites in `commonTest` exercised plain tests, nested suites,
data-driven tests, fixtures, and an assertk-only test with no `kotlin.test` import at all.

Per [`CLAUDE.md`](../CLAUDE.md) this sandbox cannot resolve AGP (`dl.google.com` blocked) or
download the Kotlin/Native distribution (`download.jetbrains.com` blocked), so the probe used
the documented Android-target-removal workaround. Where that limits a finding, it is called out
inline rather than papered over.

## 1. Target support

**Complete — all 16 of kiban's declared targets have published TestBalloon artifacts.**

Checked against the Maven Central artifact listing for `de.infix.testBalloon` rather than the
project's "supports all Kotlin target platforms" claim. Mapping kiban's target list from
`library/build.gradle.kts` onto published artifacts:

| kiban target | TestBalloon artifact |
|---|---|
| `jvm` | `testBalloon-framework-core-jvm` |
| `android` (`withHostTest`) | `testBalloon-framework-core-android` |
| `iosX64`, `iosArm64`, `iosSimulatorArm64` | `-iosx64`, `-iosarm64`, `-iossimulatorarm64` |
| `macosArm64` | `-macosarm64` |
| `tvosArm64`, `tvosSimulatorArm64` | `-tvosarm64`, `-tvossimulatorarm64` |
| `watchosArm64`, `watchosDeviceArm64`, `watchosSimulatorArm64` | `-watchosarm64`, `-watchosdevicearm64`, `-watchossimulatorarm64` |
| `linuxX64`, `linuxArm64` | `-linuxx64`, `-linuxarm64` |
| `mingwX64` | `-mingwx64` |
| `js` | `-js` |
| `wasmJs` | `-wasm-js` |

No gaps. `watchosDeviceArm64` — the target most likely to be missing from a third-party KMP
library — is published.

What was actually *executed* here is narrower than what is *published*, and the issue
anticipated this. `:library:jvmTest` ran the full suite green. `:library:compileTestKotlinJs`
and `:library:compileTestKotlinWasmJs` both compiled successfully, which proves the compiler
plugin handles JS and Wasm codegen; only the *runtime* is unreachable, because
`:kotlinNpmInstall` cannot reach the npm registry from this sandbox:

```
> Task :library:compileTestKotlinJs
> Task :library:compileTestKotlinWasmJs
BUILD SUCCESSFUL in 52s
```

Native and Android targets could not be compiled at all here (`~/.konan` download and AGP both
blocked). Those need a real CI run — `.github/workflows/gradle.yml` covers the full matrix, so
the migration PR gets that verification for free the moment it is pushed.

## 2. Kotlin version coupling — the decisive point

**This is the reason to wait rather than the reason not to go.**

TestBalloon 1.0.x publishes one artifact set *per Kotlin version*, and the Gradle plugin marker
is versioned the same way. From the project's
[CHANGELOG](https://github.com/infix-de/testBalloon/blob/main/CHANGELOG.md):

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 1.0.1-K2.4.0        | 2.4.0 … 2.4.20-Beta2      |
| 1.0.1-K2.3.20       | 2.3.20 … 2.3.21           |
| 1.0.1-K2.3.0        | 2.3.0 … 2.3.10            |
| …                   | …                         |

kiban is on Kotlin 2.4.10, which falls inside `1.0.1-K2.4.0`'s range — so adopting *today* is
possible, and that is what the probe did. The cost is structural, not immediate: from then on,
a Kotlin upgrade cannot land until a matching TestBalloon variant exists, and the version
catalog carries a `kotlin`-shaped string (`1.0.1-K2.4.0`) that `version-catalog-update` cannot
reason about. For a library whose selling point is tracking current Kotlin, that is a real
brake.

TestBalloon 1.1.0 (unreleased at the time of writing) removes it:

> This is a unified release with a single artifact set for Kotlin versions 2.3.0 and higher.
> TestBalloon's Gradle and compiler plugins auto-adapt to the Kotlin compiler version in use.
>
> * This release supports Kotlin versions 2.3.0 … 2.5.0-dev-1759, binary compatible with 1.0.1.

Since 1.1.0 is binary-compatible with 1.0.1, nothing written against 1.0.1 has to change. The
sequencing recommendation is therefore cheap: write the migration against whatever is current
when it is picked up, and prefer 1.1.0 if it has shipped by then. If the migration is wanted
sooner, `1.0.1-K2.4.0` is a working fallback, not a blocker.

## 3. Does `assertk` stay?

**Yes — keep assertk, and drop `kotlin-test` entirely.**

TestBalloon deliberately ships no assertions of its own ("TestBalloon is compatible with
existing assertion libraries"), so this is a free choice rather than a forced one. Every assertk
assertion in the probe worked unchanged inside a TestBalloon `test { … }` body, including inside
data-driven tests and fixtures. Keeping assertk means the ~1,900-line suite's assertion
vocabulary — the part that actually encodes intent — carries over untouched.

`kotlin-test` can then go. The suite uses exactly three things from it:

| `kotlin.test` API | Occurrences | TestBalloon / assertk replacement |
|---|---|---|
| `@Test` | 78 | `test("…") { … }` inside a `testSuite` |
| `assertFailsWith<T> { … }` | 40 | assertk's `assertFailure { … }.isInstanceOf<T>()` |
| `@Ignore` | 1 | `testConfig = TestConfig.disable()` |

The `assertFailure` substitution was verified, in a probe file with no `kotlin.test` import at
all:

```kotlin
val ProbeAssertkOnly by testSuite {
    test("assertFailure replaces assertFailsWith") {
        assertFailure { Modulo97.checksum("MO97") }.isInstanceOf<IllegalArgumentException>()
    }
}
```

`BUILD SUCCESSFUL`. So `libs.kotlin.test` comes out of `commonTest.dependencies` as part of the
migration; assertk stays exactly as it is.

## 4. How much of the suite has to be rewritten?

**About 44% is touched, and only 6% is genuine redesign.** Of 1,891 lines across 8 files:

- **Carries over untouched — 1,063 lines (56%).** `CountryTestData.kt` (1,034) and
  `IbanCountryTestData.kt` (29) are pure data with no framework coupling. This is the bulk of
  the suite and the migration does not touch it.
- **Mechanical — 715 lines (38%).** `IbanTest.kt` (365), `Modulo97Test.kt` (187),
  `CountryCodesTest.kt` (85), `IbanInternationalTest.kt` (78): delete the class wrapper, replace
  each `@Test fun \`name\`()` with `test("name") { … }`, swap `assertFailsWith` for
  `assertFailure`. Assertion bodies are unchanged. Tedious, low-risk, reviewable.
- **Genuine redesign — 113 lines (6%), and this is the payoff.**
  `CountryCodesParameterizedTest.kt` (70) and `IbanFieldTest.kt` (43).

The redesign bucket is drawn by *where the table is built*, not by where it is used. The
`tableOf`/`Table1` construction lives in `CountryCodesParameterizedTest.kt` alone, but there are
12 `forAll` call sites across four files — `CountryCodesParameterizedTest.kt` (2),
`IbanFieldTest.kt` (2), `IbanInternationalTest.kt` (6) and `IbanTest.kt` (2). The six in
`IbanInternationalTest.kt` and two in `IbanTest.kt` are counted as mechanical above because
those files convert body-for-body once the shared data is exposed as a plain list; they still
have to be touched as part of the same change, so the redesign should land as one commit
spanning all four files rather than file-by-file.

That last bucket is what the issue was really pointing at. Today, parameterization is
hand-rolled: a companion object builds an assertk `Table1` by folding over the country list…

```kotlin
val countriesTestDataTable =
    tableOf("Test data").run {
        var table: Table1<IbanCountryTestData>? = null
        testData.forEach { table = table?.row(it) ?: row(it) }
        requireNotNull(table)
    }
```

…which three other files then reach into by cross-file companion reference
(`CountryCodesParameterizedTest.countriesTestDataTable.forAll { … }`). In TestBalloon the whole
construct is a `for` loop:

```kotlin
val ProbeParameterized by testSuite {
    for (td in countryTestData.sortedBy { it.name }) {
        test("length for ${td.name}") { … }
    }
}
```

The measured difference is not cosmetic. Reporting counts per file, before and after:

```
CountryCodesParameterizedTest[jvm]   3 tests     (3 @Test methods, each looping 111 countries)
ProbeParameterized[jvm]            111 tests     (one real test per country)
```

`tableOf` collapses 111 countries into 3 reported tests, so a single failing country fails one
opaque test and stops the loop. TestBalloon reports `length for Albania`, `length for
Algeria`, … individually, each passing or failing on its own. That, plus deleting the
`Table1`-folding boilerplate and the cross-file companion coupling, is the concrete argument
for the migration.

Fixtures (`testFixture { … }`) are the idiomatic replacement for the shared companion-object
state, and worked in the probe.

## 5. `apiCheck` and Gradle test-task wiring

**Both clean. No changes needed to either.**

`binary-compatibility-validator`: TestBalloon's compiler plugin applies to test compilations,
and the concern is whether it leaks synthetic declarations into the main compilation's dump.
It does not. With TestBalloon applied, `:library:jvmApiDump` produced a dump **byte-identical**
to the committed `library/api/jvm/library.api`:

```
$ diff library/api/jvm/library.api library/api/library.api
IDENTICAL: TestBalloon does not alter the main JVM API surface
```

(The flat `library/api/library.api` path is the Android-target-removal workaround artifact
`CLAUDE.md` documents, not a real dump relocation.) The klib dump could not be regenerated here
— that needs Kotlin/Native — but since the JVM surface is unchanged and the plugin touches only
test compilations, there is no mechanism by which the klib dump would move. CI will confirm.

Test-task wiring: TestBalloon registers **no new test tasks**. It plugs into the existing
Kotlin Multiplatform ones — `jvmTest`, `allTests`, `jsBrowserTest`, `jsNodeTest`,
`iosX64Test`, `iosSimulatorArm64Test` and the rest are all still there under their usual names,
so `.github/workflows/gradle.yml` needs no edits. The probe run of `:library:jvmTest` executed
**193 tests, 0 failures** — the 78 pre-existing `kotlin.test` tests *and* the TestBalloon probe
suites, in the same task and the same source set.

That last detail matters for planning: the two frameworks coexist in one source set, so the
migration can land file-by-file rather than as one 1,900-line commit.

## 6. One gotcha worth recording

TestBalloon renders nested-suite names with U+00A0 NO-BREAK SPACE instead of ordinary spaces,
and Gradle's HTML report writer creates a directory per nested suite. On a machine whose locale
is not UTF-8, that combination fails the build *after* the tests themselves have passed:

```
> Could not generate test report to '…/build/reports/tests/jvmTest'.
   > Malformed input or input contains unmappable characters:
     …/jvmTest/suite_nl.bijdorpstudio.kiban.ProbeBasics/nested suite/index.html
```

The filename is `TEST-nested\302\240suite.xml` — `\302\240` being UTF-8 for U+00A0. This
sandbox's locale is `POSIX`, giving `sun.jnu.encoding = ANSI_X3.4-1968`; re-running with
`LANG=C.UTF-8` makes it pass. GitHub's `ubuntu-latest` and `macos-latest` runners are UTF-8, so
this will not bite CI. It is recorded here because it presents as a build failure with green
tests, which is a confusing thing to debug cold, and because it is a live argument for
preferring flat top-level suites over deep nesting in the migrated suite.

## Follow-up

This issue is evaluate-only; the migration itself is out of scope by its own terms. The
follow-up ticket should carry:

- Trigger: TestBalloon 1.1.0 released (or an explicit decision to go on `1.0.1-K2.4.0`).
- Drop `libs.kotlin.test` from `commonTest`; keep `libs.assertk`. Add the TestBalloon plugin and
  `testBalloon-framework-core`, plus `junit:junit` in `androidHostTest`, which TestBalloon's
  Android host-side setup requires and this sandbox could not verify.
- Migrate file-by-file (both frameworks coexist), leaving `CountryTestData.kt` and
  `IbanCountryTestData.kt` untouched.
- Replace the `tableOf`/`Table1` machinery and all 12 cross-file companion `forAll` call sites
  (`CountryCodesParameterizedTest.kt`, `IbanFieldTest.kt`, `IbanInternationalTest.kt`,
  `IbanTest.kt`) with per-item `test(…)` registration, in a single commit.
- Verify on real CI: the Native, Android, JS and Wasm test tasks, and the klib API dump, none of
  which this sandbox can run.
