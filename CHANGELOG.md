# Version History

## 0.6.0 (unreleased)

**Breaking changes**

* The minimum Kotlin version for consumers is now **2.4.0**, up from 2.3.0 (#180). Consumers who
  cannot move their compiler cannot take this release. The floor buys `@IntroducedAt`, whose rule is
  written down in [VERSIONING.md](VERSIONING.md).

* `IbanParseException.Malformed.Kind` is a sealed hierarchy instead of an enum, so three kinds carry
  typed data (`InvalidCharacter(character, index)`) and `EMPTY` becomes `Kind.Empty` (#148). Rename
  the references and list the kinds yourself: `entries`, `values()` and `valueOf(String)` are gone.

* The data-carrying `Malformed.Kind` subtypes are plain classes with internal constructors rather
  than `data class`es (#203). `copy` and `componentN` are gone, so code that built or copied a kind
  has to stop; reading `character`, `index`, `atStart` and `reason` is unaffected.

* IBAN validation and `Modulo97` are ASCII-only (#136). Input carrying non-ASCII digits or letters
  (`Iban("NL９１ABNA0417164300")` with fullwidth digits) is now rejected rather than parsed.
  Normalize with NFKC before parsing if your input can carry such characters.

* Whitespace leniency in parsing is limited to the ASCII space, 0x20 (#137). Tabs, non-breaking
  spaces and the rest are now rejected as `Malformed(INVALID_CHARACTER)`, or
  `INVALID_BOUNDARY_CHARACTER` at the ends; strip them before parsing. Interior spaces still group.

* A wrong-length input that also carries a character outside the IBAN character set is reported as
  `Malformed(INVALID_CHARACTER)` rather than `WrongLength` (#137). Such input was already rejected;
  only the diagnosis changes, and it now agrees with the correct-length path.

* A known country code written in the wrong case is reported as
  `Malformed(NON_UPPER_CASE_COUNTRY_CODE)` instead of `UnknownCountryCode` (#136). The kind is new,
  so add a branch to any exhaustive `when` over `Malformed.kind`; such input was already rejected.

* `CountryCodes.knownCountryCodes` is typed `List<String>` instead of `Collection<String>`, and is
  an immutable copy rather than a view over the library's own data (#140). Narrowing the type is
  binary-breaking on the JVM, so rebuild; Kotlin source holding it as a `Collection` still compiles.

* Length and lookup names were settled before the API freeze (#141): `Iban.SHORTEST_POSSIBLE_IBAN` →
  `SHORTEST_POSSIBLE_IBAN_LENGTH`, `CountryCodes.SHORTEST_IBAN_LENGTH` / `LONGEST_IBAN_LENGTH` →
  `shortestIbanLength` / `longestIbanLength`, and `getLength(cc)` → `ibanLength(cc)`. Rename.

* The last names that disagreed with Kotlin's conventions were settled (#204): `Iban.isSEPA` →
  `isSepa`, `CountryCodes.isSEPACountry(cc)` → `isSepaCountry(cc)`, `lastUpdateRevision` →
  `LAST_UPDATE_REVISION`. Rename; each is a compile error.

* `Iban.SHORTEST_POSSIBLE_IBAN_LENGTH` is now `internal` (#205). It was the parser's lower bound
  rather than a fact about real IBANs — measure against `CountryCodes.shortestIbanLength` instead.

**Fixes**

* `Iban.compose` now reports a malformed country code as `Malformed(INVALID_STRUCTURE)` (#146).
  A country code that was not exactly two characters bypassed validation and surfaced later as an
  `UnknownCountryCode` assembled out of BBAN characters. Input that composed before still does.

**Additions**

* Added `Iban.parse(input)`, a named alias for `Iban(input)`, and made it and `Iban.compose(...)`
  `@JvmStatic` (#139). Java and Swift callers can call them on `Iban` directly instead of going
  through `Iban.Companion.invoke`. Kotlin callers should keep using `Iban(input)`.

**Performance**

* The parse and checksum path allocates nothing beyond what the caller passed in (#206).
  `Modulo97.checksum` folds its remainder one character at a time instead of expanding the input
  into a doubled `CharArray`, chunking it and parsing each chunk. No API or behaviour change.

**Documentation**

* Added [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md) (#157): what to run before
  pushing, which checks need a toolchain most machines do not have, and the private channel for
  reporting a vulnerability, with the scope line this library needs drawn around it.

* Added [VERSIONING.md](VERSIONING.md), the versioning, compatibility and deprecation policy 1.0
  stands on (#150). It defines the public API as what the committed dumps contain, says what each
  kind of version bump means, and sets the post-1.0 deprecation cycle.

* The README states the consumer requirements as contract rather than as facts about the current
  build (#150): the Kotlin, Java bytecode and Android `minSdk` floors, and that macOS has been
  `macosArm64` only since 0.5.0. Raising any of them only happens in a major release.

* The README states that the `js` and `wasmJs` artifacts are for Kotlin/JS and Kotlin/Wasm consumers
  only (#145): nothing carries `@JsExport`, so nothing is reachable from hand-written JavaScript and
  no TypeScript definitions are generated. All of it was already true.

* Documented that `CountryCodes.lastUpdateDate` carries the registry date as the instant at midnight
  UTC, which is contract rather than an implementation detail (#144). The case for freezing `Instant`
  into the 1.0 API is in [docs/144-instant-api-stability.md](docs/144-instant-api-stability.md).

* `toIban()`, `toIbanOrNull()` and `isValidIban()` keep their `String` receivers while `Iban(...)`,
  `Modulo97` and `CountryCodes` keep taking `CharSequence` (#143). No API change; a caller holding a
  `StringBuilder` or an `Editable` passes it to `Iban(input)`. Reasoning in the KDoc and README.

* The JVM-only `typealias IBAN = Iban` stays for 1.0, scoped to `jvmMain` (#149). Common code and
  every non-JVM target see only `Iban`, which is what new code should write; the alias exists for
  migrating java-iban callers and is erased at compile time.

* Cleaned up KDoc inherited from java-iban (#108). The `Iban` class description sat after an
  `@author` tag, which KDoc swallows, so the published docs rendered it as part of the Author field;
  `@author` and two java-iban `@since` tags are gone with it. Documentation only.

* Fixed the remaining KDoc leftovers and Java-isms in the public API docs (#210): `@property` tags
  Dokka rendered twice, KDoc on a getter rather than its property, `@return` on `val`s, and
  Javadoc-style links KDoc cannot resolve. Documentation only; no signature or behaviour changed.

* Settled the Intel-target question (#207). The published target set does not change; the rule
  behind it is now policy in [VERSIONING.md](VERSIONING.md) and the evidence is in
  [docs/207-intel-target-policy.md](docs/207-intel-target-policy.md).

**Infrastructure**

* Renamed the Gradle subproject and its directory from `library` to `kiban`, so the klib unique name
  matches the published artifact (#208). The API dumps move to `kiban/api/`; the artifact
  coordinates, the published API and the sources are unchanged.

* Replaced the standalone `binary-compatibility-validator` plugin with the Kotlin Gradle plugin's
  built-in ABI validation (#182): `apiCheck` and `apiDump` are now `checkKotlinAbi` and
  `updateKotlinAbi`. See [docs/182-builtin-abi-validation.md](docs/182-builtin-abi-validation.md).

* Migrated the test suite from `kotlin.test` to TestBalloon (#115), which registers each country as
  its own test: 78 reported tests became 1,217 with no loss of coverage, so a registry update that
  breaks one country names it. See [docs/82-testballoon-evaluation.md](docs/82-testballoon-evaluation.md).

* The SWIFT registry sync runs unattended (#53): it regenerates, formats and diffs the country data
  before asking for a revision number, so the runs that find nothing finish silently. A blocked
  download now reports the manual and `--headed` fallbacks instead of a Playwright stack trace.

* CI reports what it verified (#129): per-target test results in each job summary, a roll-up in the
  `ci` gate job, failing tests annotated on the run, and the `ktfmt` and ABI dump diffs rendered
  rather than only downloadable. Every added step is non-fatal.

* Closed the remaining direct test-coverage gaps ahead of the 1.0 freeze (#149): the `hashCode`
  contract, the `SHORTEST_POSSIBLE_IBAN_LENGTH` boundary from both sides, the `CountryCodes` length
  bounds, and a `jvmTest` suite for the `IBAN` alias. Tests only.

* Enabled Kotlin's explicit API mode (strict) for the library (#142), so a declaration's visibility
  has to be stated while it is written rather than caught in a dump afterwards. Mechanical: nothing
  became public or stopped being public.

* CI runs the test suite on `mingwX64` and `linuxArm64` (#151), both published targets that had only
  ever been compiled. `linuxArm64` runs under the `qemu-aarch64` emulator the Kotlin/Native
  toolchain already ships, registered as a real test task so its results reach the `ci` roll-up.

* CI resolves the published artifact by coordinates from a build that has never heard of `:kiban`
  (#154), via `samples/consumption-probe` against `mavenLocal()` on `jvm`, `linuxX64` and `js`.
  Publishing locally needs the new `kiban.signPublications` property, which defaults to true.

* CI assembles `Kiban.xcframework` and compiles and runs `samples/swift-console` against it on every
  push and pull request (#155). A change that breaks Objective-C interop now fails on the pull
  request that made it instead of during a release run.

* The registry sync opens its data-update pull request with a `REGISTRY_SYNC_TOKEN` secret when one
  is configured (#156), so the target matrix runs on it. Unset, the job falls back to `GITHUB_TOKEN`
  and keeps the close-and-reopen note in the body.

* The `publish` job is bound to a `release` GitHub environment (#158), which adds a
  required-reviewer pause in front of the Maven Central credentials and lets the publish secrets
  move off the repository. [RELEASING.md](RELEASING.md) documents what to configure.

* Two supply-chain additions (#160): OpenSSF Scorecard, which backs the new README badge, and build
  provenance on every published `.jar`, `.klib` and `.aar`, verifiable with `gh attestation verify`.
  See [docs/160-supply-chain-posture.md](docs/160-supply-chain-posture.md).

* `scripts/generate_country_data.main.kts` grew a `--self-check` (#161) that runs the parser, the
  overlay merge and the validation against synthetic fixtures under `scripts/testdata/`, since the
  registry TXT is not redistributable. `registry-sync.yml` runs it before the download.

* Versioned API docs (#162). A release no longer overwrites the whole GitHub Pages site: the version
  being released stays at the root and earlier ones are reachable from a dropdown, archived on a
  `docs-archive` branch. See [docs/162-versioned-api-docs.md](docs/162-versioned-api-docs.md).

* TestBalloon moved from `1.1.0-RC` to the released `1.1.0` (#153), so the suite that gates
  `publish.yml` no longer runs on a release candidate. Binary compatible: no test, dump or build
  file changed with it.

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
