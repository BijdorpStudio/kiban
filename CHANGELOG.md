# Version History

## 0.6.0 (unreleased)

**Breaking changes**

* The minimum Kotlin version for consumers is now **2.4.0**, up from 2.3.0 (#180). This is a break
  for anyone who cannot move their compiler, and it lands before 1.0 precisely because it could not
  land after: raising the floor is a breaking change, so from 1.0 the same move would cost a major
  release and the whole 1.x line would go without what it buys.

  What it buys is `@IntroducedAt`. Adding a parameter with a default value to a published function
  looks source-compatible and is not binary-compatible — the signature in the artifact changes, so
  a caller compiled against the old one fails with `NoSuchMethodError` until it is rebuilt.
  Annotating the new parameter with the version that introduced it makes the compiler emit the
  older signature as a hidden overload, and both shapes land in the API dumps where they can be
  reviewed. Neither `@IntroducedAt` nor `ExperimentalVersionOverloading` resolves below a 2.4
  `languageVersion`, which is what forces the floor.

  Nothing in the library is annotated yet: no public declaration currently takes an optional
  parameter, so this buys the ability to add one during 1.x rather than fixing anything today. The
  rule is written down in [VERSIONING.md](VERSIONING.md). Note that the feature is experimental —
  `ExperimentalVersionOverloading` is `@RequiresOptIn(level = ERROR)` — and that the 2.3.0 floor's
  original reason (`kotlin.time.Instant` stabilising at 2.3, see
  [docs/144-instant-api-stability.md](docs/144-instant-api-stability.md)) is subsumed by the new
  floor but still bounds it from below.

* `IbanParseException.Malformed.Kind` is a sealed class hierarchy instead of an enum (#148). The
  kinds that could only ever be described in prose now carry that description as typed data:
  `InvalidCharacter(character, index)`, `InvalidBoundaryCharacter(character, atStart)` and
  `InvalidStructure(reason)`. A caller can react to a rejection — point at the character, count the
  kinds it sees — instead of parsing `message` for it, and the library can no longer build a
  malformed rejection that has nothing to say about itself: what used to be a runtime check when
  the rejection was turned into an exception is now a property of the type at construction.

  The entries are renamed accordingly (`EMPTY` becomes `Kind.Empty`, `TOO_SHORT` becomes
  `Kind.TooShort`, and so on), and the enum-only surface goes with them: `Kind.entries`,
  `Kind.values()` and `Kind.valueOf(String)` no longer exist, so code that enumerated the kinds
  needs to list them itself. A `when` over `Malformed.kind` stays exhaustive without an `else`,
  which is how most callers use it.

  Two rejection messages are reworded to use what the kind now carries: a boundary rejection names
  the character and the end it sits at ("Input begins with an invalid character ' ': ..."), and an
  invalid-character rejection names its index ("Invalid character '_' at index 6 in ..."). Messages
  are diagnostics, not contract, but tests that assert on them will need updating.

* IBAN validation is now ASCII-only (#136). `Iban.validate` used `Char.isDigit()` and
  `Char.isLetterOrDigit()`, and `Modulo97.checksum` used `Char.isDigit()` — all Unicode-aware on
  every platform, as is `String.toLong`, which the checksum folds its buffer through. The effect was
  that `Iban("NL９１ABNA0417164300")` (fullwidth digits) parsed successfully and produced an `Iban`
  whose `plain` held non-ASCII characters, violating the class invariant and the documented
  `[A-Za-z0-9 ]` character set. ISO 13616 defines the IBAN character set as ASCII `A-Z0-9`, so such
  input is now rejected: `Malformed(NON_NUMERIC_CHECK_DIGITS)` for non-ASCII check digits,
  `Malformed(INVALID_BOUNDARY_CHARACTER)` at the ends of the input and `Malformed(INVALID_CHARACTER)`
  elsewhere. Rejecting rather than normalizing is deliberate — input-layer concerns like fullwidth
  IME digits belong to the caller (NFKC before parsing), silently rewriting a bank account identifier
  masks upstream data corruption, and any normalization line drawn here would become frozen contract
  surface at 1.0. An opt-in normalizer stays possible later as an additive change; going lenient
  first and strict later would not.

* Whitespace leniency in parsing is now limited to the (ASCII 0x20) space (#137). `Iban.toPlain`
  stripped every character `Char.isWhitespace()` accepts, anywhere in the input, so
  `Iban("NL91\tABNA 0417164300")` parsed — while the documented contract, and `Modulo97.checksum`
  one layer down, only ever tolerated 0x20. Tabs, non-breaking spaces and the rest are now left in
  place and rejected as `Malformed(INVALID_CHARACTER)`, or `Malformed(INVALID_BOUNDARY_CHARACTER)`
  at the ends of the input. Interior spaces still group (`"NL91 ABNA 0417 1643 00"` parses) and a
  leading or trailing space is still rejected, both unchanged. Silently dropping a tab from the
  middle of a bank account identifier treats a paste artifact as intent, and the leniency cannot be
  narrowed after 1.0 without breaking callers who came to rely on it.

* A wrong-length input that also carries a character outside the IBAN character set is now reported
  as `Malformed(INVALID_CHARACTER)` rather than `WrongLength` (#137). Such a character adds to the
  length exactly like a legitimate one, so the old rejection blamed the length for a problem the
  character caused — and an input of the *correct* length carrying the same character was already
  reported as `INVALID_CHARACTER` by `Modulo97`. The two paths now agree.

* A known country code written in the wrong case is now reported as
  `Malformed(NON_UPPER_CASE_COUNTRY_CODE)` instead of `UnknownCountryCode` (#136). Lower case input
  such as `"nl91abna0417164300"` was already rejected and stays rejected (`java-iban` parity), but
  the old rejection blamed the country rather than the case: `NL` is a known country code, `nl` is
  the same country code miswritten. `NON_UPPER_CASE_COUNTRY_CODE` is a new `Malformed.Kind` entry,
  so `when` statements over `Malformed.kind` that were exhaustive will need a branch for it. An
  unknown country code that also happens to be lower case is still an `UnknownCountryCode`.

* `CountryCodes.knownCountryCodes` is now typed `List<String>` instead of `Collection<String>`
  (#140). Alphabetical order was already part of the documented contract, so the type now says so,
  and callers get indexed access without a copy. The property is also a defensive, immutable copy
  built once, rather than a fresh `Array.asList()` view per access: the old view shared storage with
  the library's own reference data, so a `MutableList` cast could `set` an entry and corrupt the
  registry for the rest of the process — it now rejects every mutation with
  `UnsupportedOperationException`, on every target rather than only on the JVM. Narrowing the return
  type is binary-breaking on JVM (`getKnownCountryCodes` changes descriptor), which is why it lands
  before 1.0; Kotlin source that stored the result in a `Collection<String>` keeps compiling.

* Constant and accessor names in `CountryCodes` and `Iban` were settled before the API freeze
  (#141). `Iban.SHORTEST_POSSIBLE_IBAN` is now `Iban.SHORTEST_POSSIBLE_IBAN_LENGTH`,
  `CountryCodes.SHORTEST_IBAN_LENGTH` / `CountryCodes.LONGEST_IBAN_LENGTH` are now
  `CountryCodes.shortestIbanLength` / `CountryCodes.longestIbanLength`, and
  `CountryCodes.getLength(cc)` is now `CountryCodes.ibanLength(cc)`. The three old length names read
  as if they held an IBAN rather than a number of characters, and `getLength` did not say the length
  of what. The `CountryCodes` pair also had the Kotlin naming convention inverted — SCREAMING_SNAKE
  non-`const` `val`s next to a lowerCamel `const val lastUpdateRevision` — which surfaced on the JVM
  as `getSHORTEST_IBAN_LENGTH()` / `getLONGEST_IBAN_LENGTH()`; lowerCamel makes them
  `getShortestIbanLength()` / `getLongestIbanLength()` and matches every other member of the object
  (`lastUpdateDate`, `lastUpdateRevision`, `knownCountryCodes`). `Iban`'s constant stays
  SCREAMING_SNAKE, which is the convention for a `const val`. Renaming after 1.0 would be breaking,
  so it happens now; every break is a compile error with a mechanical fix.

**Fixes**

* `Iban.compose` now diagnoses a malformed country code as the structural problem it is (#146). It
  assembled `countryCode + "00" + bban` and handed that to the one-argument
  `Modulo97.calculateCheckDigits`, which only checks that the characters at indices 2 and 3 are
  `'0'` — so a country code that is not exactly two characters bypassed the two-argument overload's
  country code validation. Where the BBAN happened to start with a `'0'`, as in
  `Iban.compose("N", "0417164300...")`, the check digits were computed against the wrong indices and
  the failure surfaced later as an `UnknownCountryCode` assembled out of BBAN bytes, rather than the
  `Malformed(INVALID_STRUCTURE)` that `Kind.INVALID_STRUCTURE` documents. `compose` now delegates to
  `Modulo97.calculateCheckDigits(countryCode, bban)`, which validates the country code before the
  check digit input is built. Input that composed successfully before still does; only the diagnosis
  of already-failing input changes.

**Additions**

* Added `Iban.parse(input)`, a named alias for `Iban(input)`, and made it and `Iban.compose(...)`
  `@JvmStatic` (#139). `Iban(input)` is `operator fun invoke` on the companion object, and Kotlin is
  the only language with call syntax for it: Java reads it as `Iban.Companion.invoke(...)` and — as
  `docs/9-swift-interop-review.md` confirmed against a real `Kiban.xcframework` — Swift reads it as
  `Iban.companion.invoke(input:)`, which is why `samples/swift-console` avoided it. `parse` carries
  the same `@Throws(IbanParseException::class)` contract and delegates straight to `invoke`, so
  there is no second parsing path to keep in step; `@JvmStatic` puts `parse` and `compose` directly
  on `Iban` for Java callers. The name matches the `java-iban` heritage and reads naturally next to
  `String.toIban()`. Kotlin callers should keep using `Iban(input)`. Note for anyone still on the
  0.4.0 API: the returning name is not the returning shape — 0.4.0's `Iban.parse` returned
  `Result<Iban>`, this one returns `Iban` and throws.

**Documentation**

* Added [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md) (#157). The README invited
  contributions without saying anywhere what a contributor should run, and the only build guidance in
  the repository was [CLAUDE.md](CLAUDE.md) — written for agents in cloud sandboxes, about what those
  containers can and cannot build, rather than for a human with a checkout. CONTRIBUTING.md states the
  three checks that run on any machine (`./gradlew jvmTest apiCheck ktfmtCheck`), when the API dumps
  have to be regenerated with `apiDump` and committed alongside the change, that `ktfmtFormat` is what
  fixes a formatting report, the TestBalloon/assertk shape a new test is expected to take, that the
  registry data files are generated and not to be hand-edited, and what a pull request should carry —
  including naming which targets were left unverified locally, since Apple and Android tasks need a
  toolchain most contributors do not have.

  SECURITY.md gives what a library handling bank account data should not be missing at 1.0: a private
  reporting channel, via GitHub's private vulnerability reporting rather than an email address to
  maintain. It also draws the scope line, which for this library is not obvious — validation that is
  wrong in the permissive direction is the report worth making privately, while registry data
  predating a SWIFT revision, a false negative, and the deliberately unenforced national check digits
  and format masks are ordinary issues.

* Wrote down the versioning, compatibility and deprecation policy that 1.0 has to stand on, as
  [VERSIONING.md](VERSIONING.md) (#150). The README warned that the API was unstable and
  MIGRATION.md said removed API is gone with no deprecation cycle — both true today, and both
  definitionally incompatible with a 1.0 release, with nothing written down about what replaces
  them. The new document states what a major, minor and patch bump each mean (registry data
  updates are a minor, not a patch; tightening validation is a major), defines the public API as
  what the committed dumps under `library/api/` contain and names what is deliberately outside it
  (exception messages, undocumented ordering, generated data internals), points the binary
  compatibility guarantee at the `apiCheck`/`apiDump` tooling that already enforces it, and sets
  the post-1.0 deprecation cycle: `@Deprecated` with `ReplaceWith` at `WARNING` for at least one
  further minor, optionally `ERROR` after that, removal no earlier than the next major.

* Documented the consumer requirements in the README, which were absent (#150): Kotlin 2.3.0,
  Java bytecode level 17, Android `minSdk` 24, and that macOS has been `macosArm64` only since
  0.5.0 — the last of which the CHANGELOG recorded but the README's target list did not. They are
  stated as contract rather than as facts about the current build: raising any of them only
  happens in a major release. The README's stability warning and its "Contributions & Stability"
  section, and MIGRATION.md's preamble, now point at the policy instead of at an open-ended
  expectation of breakage.

* Stated in the README that the `js` and `wasmJs` artifacts are for Kotlin/JS and Kotlin/Wasm
  consumers only (#145). No declaration in the library carries `@JsExport`, so nothing is reachable
  from hand-written JavaScript and no TypeScript definitions are generated, and the artifacts go to
  Maven Central as klibs rather than to npm. All of that was already true; it now reads as the
  scope boundary it is. Exporting to plain JS remains additive and can follow after 1.0 if it is
  wanted, whereas withdrawing an export surface could not.

* Documented that kiban requires **Kotlin 2.3.0 or newer**, and that
  `CountryCodes.lastUpdateDate` carries the registry date as the instant at midnight UTC (#144).
  Neither is a behaviour change; both were true already and now say so, because both are things a
  caller can be caught out by. `kotlin.time.Instant` only becomes a non-experimental stdlib type
  at 2.3, so a consumer compiling below that `apiVersion` is asked to opt in to read that one
  property; and the midnight-UTC encoding is contract, not an implementation detail of the getter.
  The investigation behind freezing `Instant` into the 1.0 API is in
  [docs/144-instant-api-stability.md](docs/144-instant-api-stability.md).

* Settled the `String` versus `CharSequence` receiver question for `toIban()`, `toIbanOrNull()` and
  `isValidIban()`, and wrote the reasoning down (#143). The extensions keep their `String`
  receivers, while `Iban(...)`, `Iban.parse(...)`, `Iban.compose(...)`, `Modulo97` and
  `CountryCodes` keep taking `CharSequence`: the extensions are the Kotlin-idiomatic sugar, where
  `String` is both what the stdlib's own `toInt()`/`toBoolean()` conversions use and what
  essentially every call site holds, and a `String` receiver is also the only one of the two that
  survives Kotlin/Native's Objective-C export as a typed `NSString *` parameter of the `IbanKt`
  facade — `CharSequence` has no Objective-C counterpart and erases to `id`, turning a compile
  error at the Swift call site into a runtime cast failure. Callers holding a `StringBuilder` or an
  Android `Editable` are not shut out, since `Iban(input)` and `Iban.parse(input)` accept those
  directly. No API change, so no `apiDump`: this is the "keep it and say why" half of the decision
  the issue asked for, recorded in the KDoc on `String.toIban` and in the README before the 1.0
  freeze makes the choice permanent.

* Settled the JVM-only `typealias IBAN = Iban` for 1.0 and wrote its scope into the KDoc and the
  migration guide (#149). The alias lives in `jvmMain`, so `commonMain` and every non-JVM target
  see only `Iban` — deliberate, since `java-iban` is a JVM library and the only way to surface the
  name in common code would be an `expect`/`actual` pair that puts a spelling convenience into the
  multiplatform API. It stays: a typealias is erased at compile time, so it generates no class,
  adds nothing to the API dump and costs nothing to carry, while dropping it would break source
  compatibility for exactly the migrating callers it exists for. New code should write `Iban`.

* Cleaned up KDoc inherited from `java-iban` (#108). The `Iban` class documentation had its
  construction paragraph placed after an `@author` tag, which a KDoc block tag swallows — the
  published API docs rendered that paragraph as part of the Author field instead of as the class
  description. The paragraph moved above the block tags, and the `@author` tag was dropped;
  attribution to Barend Garvelink stays where it already lives, in the per-file license headers and
  the README. Also removed `@since 1.0.0` on `Iban` and `@since 1.9.0` on
  `Modulo97.calculateCheckDigits`: those are `java-iban` versions, and kiban has never published a
  1.x release.

**Infrastructure**

* Replaced the standalone `binary-compatibility-validator` plugin with the ABI validation built into
  the Kotlin Gradle plugin (#182), acting on the investigation in
  [docs/182-builtin-abi-validation.md](docs/182-builtin-abi-validation.md). `apiCheck` and `apiDump`
  become `checkKotlinAbi` and `updateKotlinAbi`; the dumps stay where they are, in the same layout
  and format, so `library/api/library.klib.api` comes out byte-identical and every published Native,
  JS and Wasm target is covered exactly as before. The standalone plugin is in maintenance mode by
  its own README, with new work going to the built-in validation instead — nothing was broken, but
  1.0 freezes the tooling story along with the API, so the choice is better made now than after.

  `library/api/jvm/library.api` loses one line: the `public synthetic` bridge constructor Kotlin
  emits for `Iban`'s private constructor, which the built-in tool filters and the standalone one
  listed. It is `ACC_SYNTHETIC`, so no Kotlin or Java source can name it and no consumer can be
  affected. The two tools disagree on that line rather than rendering it differently, which is why
  the dump changes in the same commit and why this is a migration rather than an addition.

  Two things came out of the investigation beyond the migration itself. Enabling the built-in
  validation at all needed `libs.plugins.testballoon` declared in the root `build.gradle.kts` with
  `apply false`: applied only in `:library`, TestBalloon's transitive Kotlin Gradle plugin 2.2.0 was
  the sole KGP on that subproject's script classpath and shadowed the pinned 2.4.10, whose DSL the
  `abiValidation` block needs. And the long-standing claim that a Linux runner infers the Apple
  targets it cannot build is wrong: compiling an Apple target to a klib needs no Xcode, only linking
  a framework does, so those targets are compiled here rather than inferred. `CLAUDE.md` is
  corrected accordingly.

* Migrated the test suite from `kotlin.test` to [TestBalloon](https://github.com/infix-de/testBalloon)
  (#115), acting on the evaluation in `docs/82-testballoon-evaluation.md`. The country table used to be
  folded into an assertk `Table1` and looped inside a handful of `@Test` methods, so all 111 countries
  collapsed into 3 reported cases: a registry update that broke one country failed one opaque test and
  stopped the loop, which is exactly the report a reviewer reads first on the automated registry-sync
  pull requests. Each country now registers as its own test — `Length for Albania should return correct
  value`, `Compose should round trip Albania`, and so on — taking the suite from 78 reported tests to
  1,217 with no loss of coverage. Assertions are untouched: assertk stays, and the ~40 `assertFailsWith`
  call sites became `assertFailure { … }.isInstanceOf<…>()`. `kotlin-test` is dropped from `commonTest`
  entirely. TestBalloon is a compiler plugin plus a test-only dependency and registers no new Gradle test
  tasks, so the published artifacts still declare `kotlin-stdlib` as their only dependency and the CI
  target matrix needed no changes.

* Made the SWIFT registry sync (#53) able to run unattended. It regenerates and diffs the country data
  before asking for a registry revision, so the ~50 runs a year that find nothing finish silently and only
  a real registry change interrupts a maintainer — the release number is not published anywhere
  machine-readable, so it has to be supplied by hand when it matters. The generated sources are now
  formatted with ktfmt before diffing, without which every run reported ~1,700 whitespace-only changed
  lines and would have opened a no-op pull request. The schedule moved from monthly to weekly, and
  acquisition goes straight to headed Chromium under Xvfb: headless is dropped by Swift's edge from
  unrelated networks alike, so attempting it first spent ~60s of every run and left a failed step on
  otherwise green builds. The script itself still defaults to headless.
* `scripts/fetch_registry.main.kts` now reports a blocked download with the manual/`--headed` fallback
  advice instead of a raw Playwright stack trace: the block happens below the HTTP layer during
  navigation, so the existing response validation never saw it. It also exports the registry TXT's
  `last-modified` date for the sync workflow's logs and pull request body.

* CI now reports what it verified (#129). A passing run used to end at `BUILD SUCCESSFUL in 59s`,
  with no counts and no test names anywhere in the GitHub UI — so the per-country reporting #115
  migrated to TestBalloon for was only ever visible when something broke. Each matrix job now
  renders the JUnit XML Gradle already writes into its own job summary, and the `ci` gate job
  aggregates every target's results into a single roll-up. Test result XML is uploaded on green
  runs too, not only on failure. Failing tests are annotated on the run rather than left in the
  raw Gradle log; the reporter runs `annotate_only`, so it needs nothing beyond the workflow's
  `contents: read` token and keeps working for pull requests from forks. The `ktfmt` patch is
  rendered into the job summary instead of only being downloadable, and an `apiCheck` failure now
  gets the same treatment via `apiDump` — the dump diff being the thing a reviewer actually needs.
  Every added step is non-fatal: `ci` stays a usable required check whatever the reporting does.

* Closed the remaining direct test-coverage gaps ahead of the 1.0 freeze (#149). The `equals`
  contract was tested but `hashCode` was not, so equal-objects-equal-hash-codes, stability across
  calls, agreement across the plain and formatted spellings of the same IBAN (checked for every
  known country's example IBAN) and the behaviour that actually depends on it — equal IBANs
  collapsing in a `HashSet`, and a lookup by a distinct-but-equal key in a `HashMap` — are now
  asserted. `Iban.SHORTEST_POSSIBLE_IBAN_LENGTH` gets its value pinned and its boundary exercised
  from both sides: one character below is rejected as `TOO_SHORT`, and input exactly at the
  constant gets past the length check to fail on its country code instead.
  `CountryCodes.shortestIbanLength` / `longestIbanLength` are cross-checked against the example
  IBANs (independently of `ibanLength()`, which reads the same table), ordered against each other
  and bounded by the 34-character ISO 13616 maximum. The JVM `IBAN` alias gets a `jvmTest` suite —
  the source set did not exist before — asserting that it names the same class, reaches the
  companion functions and is assignable in both directions against `Iban`. Tests only: no
  production behaviour changed and `apiCheck` passes against the existing dumps.

* Enabled Kotlin's explicit API mode (strict) for the library (#142). Every declaration in
  `commonMain` and `jvmMain` that is part of the public API now says `public` out loud, and the
  compiler rejects any future one that doesn't state its visibility. The point is post-1.0
  maintenance: `apiCheck` catches a surface change once it has been written, but explicit API mode
  makes the author state the intent while writing it, so nothing reaches the frozen surface by
  omission. Nothing became public or stopped being public in the process — the change is
  mechanical, and `apiCheck` passes against the existing dumps unchanged. Explicit return types
  were already spelled out everywhere, so the diff is `public` modifiers only, and test sources
  are exempt as usual.

* CI now executes the test suite on `mingwX64` and `linuxArm64` (#151). Both are published
  targets, and both had only ever been *compiled* — one `cross-compile` job running
  `compileKotlinMingwX64` and `compileKotlinLinuxArm64` — so no line of the library had ever run
  on Windows or on ARM64 Linux, which made them the two targets 1.0 would have claimed support for
  on the strength of a type check alone. `mingwX64Test` now runs on a `windows-latest` runner.

  `linuxArm64` had no test task to run at all. Kotlin/Native publishes no Linux/ARM64 *host*
  compiler — the `kotlin-native-prebuilt` bundles are linux-x86_64, macos-x86_64, macos-aarch64 and
  windows-x86_64 — so an `ubuntu-24.04-arm` runner cannot build the target, and on the linux-x86_64
  host that cross-compiles it the Kotlin Gradle plugin registers nothing, because that host cannot
  execute what it produces. The toolchain that cross-compiles the target ships what it takes to run
  it anyway: `konan.properties` declares a `qemu-aarch64` user-mode emulator for the
  `linux_x64-linux_arm64` pair, and the aarch64 sysroot the binary is linked against comes down
  with the same toolchain, so emulator, sysroot and binary always match and nothing has to be
  installed. `:library` therefore registers the missing `linuxArm64Test` itself, as a
  `KotlinNativeHostTest` pointed at the emulator.

  That makes it a test task like any other rather than a bare binary run, which matters for more
  than tidiness: Gradle reads the results out of the binary's TeamCity service messages, so the
  1,582 tests land in JUnit XML, get annotated on the run and appear in the `ci` job's roll-up as
  `[linuxArm64]` beside every other target. It also makes failures fail — a Kotlin/Native test
  binary is invoked with `--ktest_no_exit_code` and exits 0 whatever the tests did, so a job that
  only checked its exit code would have reported green through a failing suite.

  The compile-only job is gone: both of its targets are now covered by a job that builds and runs
  them.

* A CI job now resolves the *published* artifact by coordinates from a build that has never heard of
  `:library` (#154). Nothing did before: `samples/jvm-cli` depends on `:library` as a project and so
  never reads a published file, and `verifyPublicationTargets` counts publications rather than
  resolving one — between them they could not tell a working publication from Gradle module metadata
  no consumer can resolve. The job publishes `kiban` to `mavenLocal()` and then runs
  `samples/consumption-probe`, a separate build that depends on
  `nl.bijdorpstudio.kiban:kiban:<version>` and exercises it on `jvm`, `linuxX64` and `js` — one
  target per compilation backend, which is the axis metadata resolution breaks along.

  Two details are what make it a real check rather than a green tick. It is *not* included from the
  root `settings.gradle.kts` and not wired up with `includeBuild`, because either would let Gradle's
  dependency substitution put `:library` back in place of the coordinates. And its repository
  declaration pins the `nl.bijdorpstudio.kiban` group to `mavenLocal()` with `exclusiveContent`,
  because the version under development is usually one Maven Central already holds — without that,
  a publish that produced nothing would resolve the last release instead and the probe would pass
  against an artifact this build did not make.

  Publishing to a local repository needs signing off (`signAllPublications()` fails on "No
  configured signatory" before writing anything, and skipping the `Sign` tasks fails on the missing
  `.asc` files instead), so `:library` gained a `kiban.signPublications` Gradle property. It
  defaults to true and `publish.yml` never sets it, so a release still signs or fails.

* CI builds the `Kiban.xcframework` and compiles and runs `samples/swift-console` against it on every
  push and pull request (#155). Neither happened before: `gradle.yml`'s matrix tests every Apple
  target but never assembles the framework, and the only thing that built the Swift sample was
  `ios-interop-verify.yml`, which is `workflow_dispatch`-only. A change that broke Objective-C
  interop — the path every consumer of the published XCFramework takes — therefore surfaced whenever
  someone next thought to press the button, in practice during a release run rather than on the pull
  request that caused it. The new `swift-console` matrix job closes that window, and because it is a
  matrix entry it joins the `ci` gate automatically. It costs one macOS runner: the XCFramework holds
  a single target (`macosArm64`), so the job is one debug link plus a `swift build`, next to four
  full Apple test jobs the matrix already runs. `ios-interop-verify.yml` keeps its copy of the same
  steps — on demand it answers what the interop *looks* like, alongside the generated Objective-C
  header, which is a different question from whether it still builds.

* The SWIFT registry sync opens its data-update pull request with a `REGISTRY_SYNC_TOKEN` secret when
  one is configured (#156). GitHub deliberately does not trigger workflow runs from events an
  automation created with the default `GITHUB_TOKEN`, so `gradle.yml` never ran on a sync pull
  request: the only verification a registry update got before merge was the `./gradlew jvmTest` the
  sync job runs itself, with "close and reopen it" written into the body as the workaround. For a
  library whose entire value is the correctness of that data, the target matrix is exactly the check
  it should get. `REGISTRY_SYNC_TOKEN` is a fine-grained PAT or GitHub App installation token scoped
  to this repository with `Contents: read and write` and `Pull requests: read and write`; the
  checkout, the branch push and `gh pr create` all use it, so the pull request comes from an identity
  whose events CI reacts to. The secret is optional — unset, the job falls back to `GITHUB_TOKEN` and
  keeps the close-and-reopen note in the body — so a fork or a repository without the secret still
  gets working sync runs rather than a failing workflow.

* The `publish` job is bound to a `release` GitHub environment (#158). It is the only job that holds
  the Maven Central credentials and the GPG signing key, and until now the sole gate in front of it
  was the release event itself: anything that could create a GitHub release could ship a signed
  artifact under this coordinate, with no second pair of eyes and nothing to stop an accidental tag.
  The environment adds the required-reviewer pause between "a release was created" and "an artifact
  is on Maven Central", and it is also what lets the five publish secrets move off the repository
  and onto the environment, where only a job declaring it can read them. Both of those are
  repository settings rather than workflow content — [RELEASING.md](RELEASING.md) documents what to
  configure, including the deployment-branch rule to avoid, since the publish runs on a tag ref.

* Two supply-chain additions for 1.0 (#160), each covering a question nothing here answered before.
  [docs/160-supply-chain-posture.md](docs/160-supply-chain-posture.md) records the choices, the
  options that were rejected, and why the issue's third item needed no change: **CodeQL** is
  already enabled on this repository through default setup, which analyses `java-kotlin` and
  `actions` on every pull request without leaving a file in `.github/workflows` to notice. An
  advanced workflow cannot be added alongside it — GitHub rejects the upload with "CodeQL analyses
  from advanced configurations cannot be processed when the default setup is enabled" — so
  replacing it would be a repository-settings decision, not a pull request.

  **OpenSSF Scorecard** (`.github/workflows/scorecard.yml`) scores the repository against the
  OpenSSF checks, files the findings as code scanning alerts, and publishes the result, which is
  what backs the new README badge and what a consumer querying the Scorecard API or deps.dev reads.
  Default branch only: the score is a property of `main`, and publishing works from nowhere else.

  **Build provenance** on the published artifacts. The GPG signature on a Maven Central artifact
  says a maintainer's key signed something; it does not say which commit or which workflow produced
  the bytes. `publish.yml` now attests every `.jar`, `.klib` and `.aar` it uploads, so
  `gh attestation verify <file> --repo BijdorpStudio/kiban` answers that against a file pulled from
  Maven Central. A Kotlin Multiplatform publication leaves its 17 modules' artifacts in as many
  places, so the job stages the publication into one local Maven repository inside the workspace
  first — a second write of already-built files, not a second build. No CycloneDX SBOM: kiban
  depends on nothing but the Kotlin standard library and the POM already says so.

* `scripts/generate_country_data.main.kts` grew a `--self-check` (#161), so the generator's parser is
  no longer the one piece of the registry pipeline with no offline test. The SWIFT registry TXT is
  not redistributable, so there is no real fixture to commit and a format-handling regression could
  only surface during an actual weekly sync — against whatever the registry happened to be that
  week. Two synthetic fixtures under `scripts/testdata/` stand in: invented countries on ISO 3166
  user-assigned codes (`XA`–`XH`), carrying the quirks the parser has to survive — identifier ranges
  and absent identifiers, whitespace inside cells and around them, an example IBAN in print format,
  a row that stops short of the last country, a cell quoted across two lines, and a registry
  stripped to the rows the parser cannot do without. The check runs the parser, the overlay merge
  and the validation, asserts the corruptions validation has to reject, and writes nothing;
  `registry-sync.yml` runs it before the download, so a broken parser fails the run instead of
  generating from it. Validation itself changed with it: an entry whose example and identifier
  positions disagree on a length is now reported as out of range rather than dying in `substring()`.

* Versioned API docs (#162). Every release used to overwrite GitHub Pages with the Dokka output of
  the version being published, so a consumer still on an older line lost their reference the moment
  the next release went out. `publish.yml` now generates the docs with Dokka's versioning plugin:
  the version being released stays at the root — the README badge keeps landing on the latest docs —
  and every previous version is reachable from a dropdown. The archive of older versions is the
  previously published site, kept on a `docs-archive` branch (rebuilt as a single commit each
  release, so it never accumulates history) rather than in `main`, where a full Dokka site per
  release would dominate every clone. The job that pushes that branch is separate from the one that
  runs Gradle, so `contents: write` never sits on a job running project build logic. See
  [docs/162-versioned-api-docs.md](docs/162-versioned-api-docs.md).

## 0.5.0

**Breaking changes**

* Reverted parsing from `Result`-returning (introduced in 0.4.0) back to strict and throwing. `Iban(input)`,
  `Iban.compose(cc, bban)` and `String.toIban()` now return `Iban` directly and throw a sealed
  `IbanParseException` on invalid input, instead of returning `Result<Iban>`. 0.4.0's `Result` shape did not
  survive the trip to Swift (#9); this corrects it before a second published version carries it forward. The
  throwing entry points are annotated `@Throws(IbanParseException::class)`, which Kotlin/Native's Objective-C
  exporter needs to surface a catchable Swift error instead of aborting the process.
* Removed `Iban.parse` and `Iban.valueOf` — use `Iban(input)` or `String.toIban()`.
* Removed `Iban.format` and `Iban.toPretty` — parse and use `iban.pretty` instead.
* Removed every other `@Deprecated` member: `Iban.toPlainString()`, `CountryCodes.getBankIdentifier(Iban)`,
  `CountryCodes.getBranchIdentifier(Iban)`, `CountryCodes.getLengthForCountryCode(cc)`, and
  `CountryCodes.lastUpdateDateString`. See [MIGRATION.md](MIGRATION.md) for the full mapping.

**Targets**

* Removed `macosX64`, `tvosX64` and `watchosX64`, which JetBrains deprecated in Kotlin 2.3.20.
  `iosX64` is not part of that deprecation and stays. This is a breaking change for consumers on
  Intel Macs: the macOS artifact and the macOS slice of the `Kiban` XCFramework are now
  `macosArm64` only.

**Infrastructure**

* Added `samples/`: a `jvm-cli` app (depending on `:library` directly) that walks through every
  code example from this README's "Use" section as a runnable demo, and a `swift-console` Swift
  Package Manager executable exercising the library through Kotlin/Native's Objective-C interop
  — the concrete testbed for the Swift-API review in #9. `jvm-cli` runs on every PR;
  `swift-console` builds on demand via `ios-interop-verify.yml`.

## 0.4.0

**Breaking changes.** The API was reshaped while there are no external consumers.

* `Iban.parse`, `Iban.compose` and `Iban(...)` now return `Result<Iban>` instead of throwing. Failures carry a
  sealed `IbanParseException` — `Malformed` (with a `kind`), `UnknownCountryCode`, `WrongLength`,
  `WrongChecksum` — which extends `IllegalArgumentException`, so `getOrThrow()` matches the old behaviour.
* `String.toIban()` returns `Result<Iban>`; added `String.toIbanOrNull()`.
* Removed `IBANFields`; use `Iban.bankIdentifier` and `Iban.branchIdentifier`.
* `CountryCodes.lastUpdateDate` is a `kotlin.time.Instant` from the standard library, and the kotlinx-datetime
  dependency is gone. The library now has no dependencies beyond the Kotlin standard library.

**Fixes**

* `Iban.compose` produced an invalid IBAN for every country whose check digits are 10 or higher: the computed
  digits were discarded and `00` was left in place, so the result failed its own checksum validation.

**Data**

* SWIFT IBAN Registry updated from rev 97 (2024-05-25) to rev 102. Yemen added; Honduras promoted into the
  registry; Poland's 8-digit routing code reclassified from branch to bank identifier; Jordan gained a branch
  identifier; AL, MD, ME, MK and RS flagged as SEPA.
* Added `scripts/generate_country_data.main.kts`, which regenerates the country data and its test table from the
  registry TXT with cross-validation against the registry's own identifier examples.

**Targets**

* Added `wasmJs`, `macosX64`, `macosArm64`, `tvosX64`, `tvosArm64`, `tvosSimulatorArm64`, `watchosX64`,
  `watchosArm64`, `watchosDeviceArm64`, `watchosSimulatorArm64`, `linuxArm64` and `mingwX64`, and the browser
  environment for `js`.

**Documentation**

* Added installation instructions, `MIGRATION.md`, and a changelog entry for 0.3.0.

## 0.3.0

First release published to Maven Central, as `nl.bijdorpstudio.kiban:kiban`.

* Kotlin-idiomatic API alongside the java-iban one: `plain` and `pretty` properties, `bankIdentifier` and
  `branchIdentifier` on `Iban`, `Iban(...)` as an operator, and the `String.toIban()` / `String.isValidIban()`
  extensions.
* `CountryCodes.getLength` returns `Int?` instead of `-1` for unknown country codes; `lastUpdateDate` became a
  date type rather than a string; `IBAN.toPretty` became `Iban.format`.
* The java-iban API is kept as a deprecated compat layer with `ReplaceWith` migrations: the `IBAN` typealias,
  `valueOf`, `toPlainString`, `toPretty`, `getLengthForCountryCode` and `lastUpdateDateString`.
* Binary compatibility validation (`apiCheck`) added for the JVM and klib targets.

## 0.2.0
* Parity of API with java library complete (except from the ancient Java version)

## 0.1.0
* Initial conversion from java to kotlin for [java-iban](https://github.com/barend/java-iban) library
