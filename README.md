<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo-dark-trimmed.png">
  <img src="assets/logo-light-trimmed.png" alt="kiban" width="169">
</picture>

# **K**Iban—Kotlin Multiplatform IBAN Library

[![Maven Central](https://img.shields.io/maven-central/v/nl.bijdorpstudio.kiban/kiban.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/nl.bijdorpstudio.kiban/kiban)
[![CI](https://github.com/BijdorpStudio/kiban/actions/workflows/gradle.yml/badge.svg)](https://github.com/BijdorpStudio/kiban/actions/workflows/gradle.yml)
[![API docs](https://img.shields.io/badge/API%20docs-Dokka-blue.svg)](https://bijdorpstudio.github.io/kiban/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Introduction

This Kotlin Multiplatform library is a continuation and re-implementation of the original [`java-iban`](https://github.com/barend/java-iban) library by Barend Garvelink. It delivers IBAN validation, formatting, and country-specific IBAN details. The library is aimed to fulfill the same features as the original but in a Kotlin Multiplatform environment.

> ⚠ **Important Note**: The API of this library is still evolving and not yet stable. Expect breaking changes until the API stabilizes in a future release.

### Background

The original [`java-iban`]((https://github.com/barend/java-iban)) library laid a solid foundation for IBAN validation and utility functions in Java environments. This library reimagines those capabilities with Kotlin's cross-platform features, making it ready for use on multiple platforms such as JVM, Android, iOS, and more.

## Features

- Validate International Bank Account Numbers (IBANs).
- Retrieve country-specific IBAN details.
- No dependencies beyond the Kotlin standard library.
- Multiplatform compatibility across Kotlin-supported targets.

## Installation

Artifacts are published to Maven Central.

``` kotlin
dependencies {
    implementation("nl.bijdorpstudio.kiban:kiban:0.5.0")
}
```

In a multiplatform project, add it to `commonMain`:

``` kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("nl.bijdorpstudio.kiban:kiban:0.5.0")
        }
    }
}
```

Supported targets: JVM, Android, `js` (Node.js and browser), `wasmJs` (Node.js and browser), iOS, macOS, watchOS, tvOS, `linuxX64`, `linuxArm64`, and `mingwX64`.

The `js` and `wasmJs` artifacts serve **Kotlin/JS and Kotlin/Wasm consumers**, not plain
JavaScript or TypeScript ones. Nothing in the library is annotated `@JsExport`, so none of its
declarations are reachable from hand-written JavaScript, and no TypeScript definitions are
generated for them; the artifacts are published to Maven Central as Kotlin klibs for a Gradle
build to resolve, not to npm. That is a deliberate scope boundary rather than an omission:
`@JsExport` accepts only a subset of Kotlin types, and once names, overloads and nullability are
visible to JavaScript they become a second frozen contract to maintain alongside the Kotlin one.
Exporting to plain JS stays possible as a purely additive change after 1.0 if there is demand for
it — withdrawing it again would not be.

Requires **Kotlin 2.3.0 or newer**. One API depends on that floor rather than merely being
built against it: `CountryCodes.lastUpdateDate` returns `kotlin.time.Instant`, which the
standard library only makes non-experimental from 2.3. Consumers compiling with an
`apiVersion` below 2.3 can use the rest of the library, but reading that one property will ask
them for `@OptIn(kotlin.time.ExperimentalTime::class)`. See
[docs/144-instant-api-stability.md](docs/144-instant-api-stability.md).

## Use

Parsing is strict: invalid input throws a typed `IbanParseException`, so you don't need to unwrap a
`Result` for the common case. The exception-free `toIbanOrNull()` and `isValidIban()` are there for
when you want to check input without paying for a stack trace.

Input must be ASCII. ISO 13616 defines the IBAN character set as `A-Z0-9`, so upper case ASCII
letters, ASCII digits and (ASCII 0x20) spaces are all that parse; non-ASCII look-alikes such as
fullwidth `９` or Arabic-Indic `٩` digits are rejected rather than normalized, because silently
rewriting a bank account identifier hides upstream data corruption. NFKC-normalize user input before
parsing if your input layer can produce them.

The whitespace leniency is just as narrow: the (ASCII 0x20) space is ignored between the first and
last character, so both `"NL91ABNA0417164300"` and `"NL91 ABNA 0417 1643 00"` parse, but a leading
or trailing space is rejected and so is any other whitespace anywhere — a tab or a non-breaking
space mid-IBAN is a paste artifact, not grouping, and is reported as an invalid character. Trim
before parsing if your input layer can produce them.

``` kotlin
    // The primary entry point. Throws IbanParseException on invalid input.
    val iban: Iban = Iban( "NL91ABNA0417164300" )

    // Or use the String extension; same throwing behaviour.
    val parsed: Iban = "NL91ABNA0417164300".toIban()

    // Exception-free fast paths.
    val orNull: Iban? = "NL91ABNA0417164301".toIbanOrNull() // null, check digits are wrong
    val isValid: Boolean = "NL91ABNA0417164300".isValidIban() // true

    // Failures carry a typed reason, so you never have to match on messages.
    try {
        Iban( input )
    } catch ( failure: IbanParseException ) {
        when ( failure ) {
            is IbanParseException.UnknownCountryCode -> reportUnknown( failure.countryCode )
            is IbanParseException.WrongLength -> reportLength( failure.expectedLength, failure.actualLength )
            is IbanParseException.WrongChecksum -> reportChecksum()
            is IbanParseException.Malformed -> reportMalformed( failure.kind )
        }
    }

    // toString() emits standard formatting, plain is compact.
    val formatted = iban.toString() // "NL91 ABNA 0417 1643 00"
    val plain = iban.plain // "NL91ABNA0417164300"

    // Input may be formatted.
    val anotherIban = Iban( "BE68 5390 0754 7034" )

    // Iban implements Comparable<T>.
    val ibans = getListOfIBANs()
    ibans.sorted() // sorts in lexical order

    // The equals() and hashCode() methods are implemented.
    val ibansAsKeys = mutableMapOf<Iban, String>()
    ibansAsKeys.put( iban, "this is fine" )

    // You can use the Modulo97 class directly to compute or verify the check digits on an input.
    val candidate = "GB29 NWBK 6016 1331 9268 19"
    val valid = Modulo97.verifyCheckDigits( candidate ) // true

    // Compose the IBAN for a country and BBAN; also throws on invalid input.
    Iban.compose( "BI", "10000100010000332045181" ) // BI4210000100010000332045181

    // You can query whether an IBAN is of a SEPA-participating country
    val isSepa = Iban( candidate ).isSEPA // true

    // You can query whether an IBAN is in the SWIFT Registry
    val isRegistered = Iban( candidate ).isInSwiftRegistry // true

    // Modulo97 API methods take CharSequence, not just String.
    val builder = StringBuilder( "LU000019400644750000" )
    val checkDigits = Modulo97.calculateCheckDigits( builder ) // 28

    // Modulo97 API can calculate check digits, also for non-iban inputs.
    // It does assume/require that the check digits are on indices 2 and 3.
    Modulo97.calculateCheckDigits( "GB", "NWBK60161331926819" ) // 29
    Modulo97.calculateCheckDigits( "XX", "X" ) // 72

    // Get the expected IBAN length for a country code:
    val expectedLength: Int? = CountryCodes.ibanLength( "DK" ) // 18

    // Get the Bank Identifier and Branch Identifier:
    val bankId: String? = iban.bankIdentifier
    val branchId: String? = iban.branchIdentifier

    // Get the BBAN, the counterpart of what Iban.compose() takes:
    val bban: String = iban.bban // round trips: Iban.compose( iban.countryCode, iban.bban )
```

`Modulo97` is the one part of the library that still throws unconditionally: its inputs are
programmer-supplied, so a bad one is a contract violation rather than user input to be validated.
`Iban`'s throwing entry points (`Iban(...)`, `Iban.compose(...)`, `String.toIban()`) are annotated
`@Throws(IbanParseException::class)`, so Kotlin/Native's Objective-C exporter emits an `NSError**`
out-parameter and Swift sees a normal `throws` function instead of the process aborting on an
unannotated exception.

`Iban(input)` is `operator fun invoke` on the companion object, which only Kotlin has call syntax
for: Java reads it as `Iban.Companion.invoke(...)` and Swift as `Iban.companion.invoke(input:)`.
For those callers there is `Iban.parse(input)`, a named alias that parses identically, and
`Iban.compose(...)` is `@JvmStatic` for the same reason. Kotlin callers should keep using
`Iban(input)`.

`Iban(...)`, `Iban.parse(...)`, `Iban.compose(...)`, `Modulo97` and `CountryCodes` all take
`CharSequence`, while `toIban()`, `toIbanOrNull()` and `isValidIban()` are extensions on `String`.
That split is deliberate. The extensions are the Kotlin sugar, and Kotlin's own conversion
extensions (`toInt()`, `toBoolean()`) are declared on `String` too; a `String` receiver also exports
as an `NSString *` parameter of the generated `IbanKt` facade, so `IbanKt.toIban(_:)` stays
type-checked from Swift, where a `CharSequence` receiver would erase to an untyped `id`. If you hold
something else — a `StringBuilder`, an Android `Editable` — `Iban(input)` and `Iban.parse(input)`
take it as-is; for the exception-free pair, convert first (`builder.toString().toIbanOrNull()`).

Migrating from `java-iban`, from kiban 0.3.0 and earlier, or from the `Result`-returning 0.4.0 API?
See [MIGRATION.md](MIGRATION.md).

Every example above is walked through by [`samples/jvm-cli`](samples/jvm-cli), a runnable demo
of the API; see [`samples/`](samples) for that and a Swift consumer exercising the library
through Kotlin/Native's Objective-C interop — see
[`docs/9-swift-interop-review.md`](docs/9-swift-interop-review.md) for the review that found the
0.4.0 `Result`-returning API didn't survive the trip to Swift, which is what this strict,
`@Throws`-annotated API is meant to fix.

## Design choices

### Java IBAN library

I [(Barend)](https://github.com/barend) like the Joda-Time library, and I try to follow the same design principles. I'm explicitly targetting Android, which at the time this library started was still on Java 1.6. I'm trying to keep the library as simple as I can.
* Easy to integrate: don't bring transitive dependencies. The KMP variant follows this too: it depends only on the Kotlin standard library.
* The `Iban` objects are immutable, and the Iban therein is non-empty and valid. There is no support for partial or invalid IBANs. Note that "valid" isn't as strict as it could be:
  * It checks that the length is correct (varies per country) and that the check digits are correct.
  * The national format mask (such as `QA2!n4!a21!c`) is not enforced. This seems to me like more work than necessary. The modulo-97 checksum catches most input errors anyway, and I don't want to force a memory-hungry regex check onto Android users. Speaking of Android, this mask could be used for keyboard switching on an Iban EditText, but that's for a different open-source project.
  * Any national check digits are not enforced. Doing this right is more work than I want to put into this. I lack the country-specific knowledge of all the gotchas and intricacies. If other countries' check digits are anything like those in the Netherlands, they're going to differ by Bank Identifier.
* There is no way to configure extra restrictions such as "only SEPA countries" on the `Iban.parse()` method. This, to me, would look too much like Joda-Time's pluggable `Chronology` system, which leads to PoLS violations (background: [Why JSR-310 isn't Joda-Time](https://blog.joda.org/2009/11/why-jsr-310-isn-joda-time_4941.html)).
* There is no class to represent a partially entered IBAN or a potentially-invalid IBAN. I'm sure there are use cases where you want to shift this sort of data around. As far as this library is concerned, if it's not an Iban it's just a string, and there already exist data types for dealing with those.
* Any feature that's not present in all IBAN's is kept outside the `Iban` class. Currently, that's the support for extracting Bank and Branch identifiers, which lives in the `CountryCode` class.
* The library originally supported an SDK 14 (Ice Cream Sandwich) era Android app. This is why it relies on bit-packing to reduce bytecode size.

### Kotlin library

Adopted design choices from the Java library, plus:
* Kotlinize the API so it is idiomatic for Kotlin users.
* Parsing is strict and throws a sealed `IbanParseException` on invalid input, rather than returning a `Result`. The exception type extends `IllegalArgumentException`, and callers who want typed errors can catch it and inspect the failure instead of matching on messages. Every throwing entry point carries `@Throws(IbanParseException::class)`, which is load-bearing for Kotlin/Native's Objective-C interop: an exception escaping an unannotated function aborts the process there, rather than surfacing as a catchable Swift error.
* `Modulo97` keeps throwing: it is a low-level utility whose errors indicate a contract violation, not invalid user input.
* Zero dependencies: only the Kotlin standard library, which is what lets the library ship on every Kotlin target.

## Updating the IBAN registry data

The embedded country data (`CountryCodesData.kt`) and the country test data table are generated from the SWIFT IBAN Registry TXT. The registry TXT is not redistributable and is never committed: it lives only in the gitignored `scripts/input/`.

* **Automatically:** the [SWIFT registry sync](.github/workflows/registry-sync.yml) workflow runs weekly (and on demand), downloads the registry through a real Chromium context, regenerates the data, and opens a pull request when it changed. Bot detection is outside this project's control, so treat it as convenience rather than a guarantee.
* **Manually,** which always works and is the fallback when the workflow is blocked:

```shell
# Download "IBAN Registry (TXT)" in a browser from https://www.swift.com/standards/data-standards/iban,
# or try the scripted download. Headless Chromium is blocked by Swift's bot detection from at least
# some networks (which is why CI runs headed under Xvfb), so reach for --headed when this times out:
kotlin scripts/fetch_registry.main.kts --out scripts/input/iban-registry.txt

kotlin scripts/generate_country_data.main.kts --registry scripts/input/iban-registry.txt --rev <revision>

# The generator emits unformatted KotlinPoet output; without this the diff is thousands of
# whitespace-only lines:
./gradlew :library:ktfmtFormatKmpCommonMain :library:ktfmtFormatKmpCommonTest
```

The registry's release number has to be supplied by hand: the download endpoint sends no filename and the registry page states no release, so neither script can detect it. Read it off the [registry PDF](https://www.swift.com/swift-resource/9606/download) and pass it as `--rev`.

The generator validates every entry before writing (mod-97 checksum, declared length and country prefix, and bank/branch identifier positions cross-checked against the registry's own identifier examples), so a malformed or truncated download fails loudly instead of landing in the library.

## References

* [SWIFT IBAN page](https://www.swift.com/standards/data-standards/iban) — official ISO 13616 registry page
  * [IBAN Registry (PDF)](https://www.swift.com/swift-resource/9606/download)
  * [IBAN Registry (TXT, machine-readable)](https://www.swift.com/swift-resource/11971/download) — note: downloads only work from a real browser; plain HTTP clients are blocked
* [SEPA Participants](https://www.europeanpaymentscouncil.eu/document-library/other/epc-list-sepa-scheme-countries)
* [Experimental IBANs](https://www.iban.com/structure)
* [General Information](http://en.wikipedia.org/wiki/IBAN)

## Contributions & Stability

As this is still an evolving library with an unstable API, contributions are welcome! Join the development journey and help shape a modern, multiplatform IBAN utility library.

## License

This project follows the same licensing model as the original library and is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).