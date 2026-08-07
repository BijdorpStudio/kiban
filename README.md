<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo-dark.png">
  <img src="assets/logo-light.png" alt="kiban" width="420">
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
    implementation("nl.bijdorpstudio.kiban:kiban:0.4.0")
}
```

In a multiplatform project, add it to `commonMain`:

``` kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("nl.bijdorpstudio.kiban:kiban:0.4.0")
        }
    }
}
```

Supported targets: JVM, Android, `js` (Node.js and browser), `wasmJs` (Node.js and browser), iOS, macOS, watchOS, tvOS, `linuxX64`, `linuxArm64`, and `mingwX64`.

## Use

Parsing returns a `kotlin.Result`, so invalid input is a value rather than an exception. Nothing in the library throws for bad user input.

``` kotlin
    // Parse returns Result<Iban>.
    val iban: Iban = Iban.parse( "NL91ABNA0417164300" ).getOrThrow()

    // Handle failure without exceptions.
    Iban.parse( input ).fold(
        onSuccess = { accept( it ) },
        onFailure = { showError( it.message ) }
    )

    // Or use the String extensions.
    val parsed: Result<Iban> = "NL91ABNA0417164300".toIban()
    val orNull: Iban? = "NL91ABNA0417164301".toIbanOrNull() // null, check digits are wrong
    val isValid: Boolean = "NL91ABNA0417164300".isValidIban() // true

    // Failures carry a typed reason, so you never have to match on messages.
    when ( val failure = Iban.parse( input ).exceptionOrNull() ) {
        is IbanParseException.UnknownCountryCode -> reportUnknown( failure.countryCode )
        is IbanParseException.WrongLength -> reportLength( failure.expectedLength, failure.actualLength )
        is IbanParseException.WrongChecksum -> reportChecksum()
        is IbanParseException.Malformed -> reportMalformed( failure.kind )
        null -> Unit // parsed successfully
    }

    // toString() emits standard formatting, plain is compact.
    val formatted = iban.toString() // "NL91 ABNA 0417 1643 00"
    val plain = iban.plain // "NL91ABNA0417164300"

    // Input may be formatted.
    val anotherIban = Iban.parse( "BE68 5390 0754 7034" ).getOrThrow()

    // Iban implements Comparable<T>.
    val ibans = getListOfIBANs()
    ibans.sorted() // sorts in lexical order

    // The equals() and hashCode() methods are implemented.
    val ibansAsKeys = mutableMapOf<Iban, String>()
    ibansAsKeys.put( iban, "this is fine" )

    // You can use the Modulo97 class directly to compute or verify the check digits on an input.
    val candidate = "GB29 NWBK 6016 1331 9268 19"
    val valid = Modulo97.verifyCheckDigits( candidate ) // true

    // Compose the IBAN for a country and BBAN; this also returns a Result.
    Iban.compose( "BI", "10000100010000332045181" ).getOrThrow() // BI4210000100010000332045181

    // You can query whether an IBAN is of a SEPA-participating country
    val isSepa = Iban.parse( candidate ).getOrThrow().isSEPA // true

    // You can query whether an IBAN is in the SWIFT Registry
    val isRegistered = Iban.parse( candidate ).getOrThrow().isInSwiftRegistry // true

    // Modulo97 API methods take CharSequence, not just String.
    val builder = StringBuilder( "LU000019400644750000" )
    val checkDigits = Modulo97.calculateCheckDigits( builder ) // 28

    // Modulo97 API can calculate check digits, also for non-iban inputs.
    // It does assume/require that the check digits are on indices 2 and 3.
    Modulo97.calculateCheckDigits( "GB", "NWBK60161331926819" ) // 29
    Modulo97.calculateCheckDigits( "XX", "X" ) // 72

    // Get the expected IBAN length for a country code:
    val expectedLength: Int? = CountryCodes.getLength( "DK" ) // 18

    // Get the Bank Identifier and Branch Identifier:
    val bankId: String? = iban.bankIdentifier
    val branchId: String? = iban.branchIdentifier
```

`Modulo97` is the one part of the library that still throws: its inputs are programmer-supplied, so a bad one is a contract violation rather than user input to be validated.

Migrating from `java-iban` or from kiban 0.3.0 and earlier? See [MIGRATION.md](MIGRATION.md).

Every example above is walked through by [`samples/jvm-cli`](samples/jvm-cli), a runnable demo
of the API; see [`samples/`](samples) for that and a Swift consumer exercising the library
through Kotlin/Native's Objective-C interop.

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
* Parsing reports failure as `Result.failure` carrying a sealed `IbanParseException`, rather than by throwing. The exception type extends `IllegalArgumentException`, so `getOrThrow()` behaves exactly like the old throwing API, and callers who want typed errors can inspect the failure instead of matching on messages.
* `Modulo97` keeps throwing: it is a low-level utility whose errors indicate a contract violation, not invalid user input.
* Deprecate old API and provide an automatic migration mechanism through `ReplaceWith`. The compat layer is kept until 1.0.
* Zero dependencies: only the Kotlin standard library, which is what lets the library ship on every Kotlin target.

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