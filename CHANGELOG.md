# Version History

## 0.6.0 (unreleased)

**Infrastructure**

* Made the SWIFT registry sync (#53) able to run unattended. It regenerates and diffs the country data
  before asking for a registry revision, so the ~50 runs a year that find nothing finish silently and only
  a real registry change interrupts a maintainer — the release number is not published anywhere
  machine-readable, so it has to be supplied by hand when it matters. The generated sources are now
  formatted with ktfmt before diffing, without which every run reported ~1,700 whitespace-only changed
  lines and would have opened a no-op pull request. The schedule moved from monthly to weekly.
* `scripts/fetch_registry.main.kts` now reports a blocked download with the manual/`--headed` fallback
  advice instead of a raw Playwright stack trace: the block happens below the HTTP layer during
  navigation, so the existing response validation never saw it. It also exports the registry TXT's
  `last-modified` date for the sync workflow's logs and pull request body.

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
