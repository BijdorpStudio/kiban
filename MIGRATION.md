# Migration guide

Kiban is a port of [`java-iban`](https://github.com/barend/java-iban). This document maps the old API onto the
current one, for people arriving from `java-iban`, from kiban 0.3.0 or earlier, or from the `Result`-returning
kiban 0.4.0 API.

Every break mapped below happened before 1.0, while the API was still being settled: there was no deprecation
cycle and no compat layer, so the left-hand column is gone rather than deprecated, and each break is a compile
error rather than a runtime surprise. That is not how removals work from 1.0 onwards — see
[VERSIONING.md](VERSIONING.md) for the semver contract, the binary compatibility guarantee and the deprecation
cycle a post-1.0 removal has to run first. Kiban has not reached 1.0 yet, so the remaining 0.x releases can
still add to this guide.

## Upgrading from kiban 0.4.0 to 0.5.0

0.4.0 made `Iban.parse`, `Iban(...)`, `String.toIban()` and `Iban.compose` return `Result<Iban>` instead of
throwing. That shape didn't survive the trip to Swift (see
[`docs/9-swift-interop-review.md`](docs/9-swift-interop-review.md)), so 0.5.0 reverts it: parsing is strict again
and throws a typed `IbanParseException`. 0.5.0 also removes every member that was `@Deprecated` in 0.4.0 and
earlier — there are no replacements to reach for beyond what's listed here.

| From | To |
| --- | --- |
| `Iban.parse(s)` | `Iban(s)` — 0.6.0 brings the name back, but it returns `Iban` and throws, it does not return `Result<Iban>` |
| `Iban.parse(s).getOrThrow()` | `Iban(s)` |
| `Iban.parse(s).getOrNull()` | `s.toIbanOrNull()` |
| `Iban.parse(s).exceptionOrNull()` | `try { Iban(s) } catch (e: IbanParseException) { … }` |
| `Iban.parse(s).fold(...)` | `try`/`catch`, or `runCatching { Iban(s) }.fold(...)` |
| `Iban(s)` *(returned `Result`)* | `Iban(s)` — now returns `Iban` directly, throws |
| `"...".toIban()` *(returned `Result`)* | `"...".toIban()` — now returns `Iban` directly, throws |
| `Iban.valueOf(s)` | `Iban(s)` |
| `Iban.compose(cc, bban).getOrThrow()` | `Iban.compose(cc, bban)` |
| `Iban.format(s)` | no replacement — parse, then use `iban.pretty` |
| `Iban.toPretty(s)` / `Iban.toPlain(s)` | no replacement — parse, then `iban.pretty` / `iban.plain` |
| `iban.toPlainString()` | `iban.plain` |
| `CountryCodes.getBankIdentifier(iban)` | `iban.bankIdentifier` |
| `CountryCodes.getBranchIdentifier(iban)` | `iban.branchIdentifier` |
| `CountryCodes.getLengthForCountryCode(cc)` | `CountryCodes.ibanLength(cc) ?: -1` |
| `CountryCodes.lastUpdateDateString` | `CountryCodes.lastUpdateDate` |

`runCatching { Iban(s) }` reproduces the old `Result`-returning behaviour exactly, for anyone who wants it back
locally without touching the public API.

## Package and type names

| java-iban | kiban |
| --- | --- |
| `nl.garvelink.iban` | `nl.bijdorpstudio.kiban` |
| `IBAN` | `Iban` (a `typealias IBAN = Iban` exists on JVM) |
| `Modulo97` | `Modulo97` |
| `CountryCodes` | `CountryCodes` |
| `IBANFields` | removed — use `Iban.bankIdentifier` / `Iban.branchIdentifier` |
| `IBANFieldsCompat` | never ported |

The `IBAN` alias is JVM-only: it is declared in the library's `jvmMain` source set, so Kotlin code
compiled for the JVM (or for Android) sees it, while `commonMain` and the non-JVM targets see only
`Iban`. It stays in 1.0 — a typealias is erased at compile time, so it costs nothing to carry —
but new code should be written against `Iban`.

## Parsing

`Iban(input)` — or the `invoke` operator's spelling, `Iban.invoke(input)` — is the primary entry point. It
validates the input and confirms the check digits, throwing `IbanParseException` on any failure.

| Before | Now |
| --- | --- |
| `IBAN.valueOf(input)` | `Iban(input)` |
| `IBAN.parse(input)` | `Iban(input)` |
| `try { IBAN.valueOf(s) } catch (e: IllegalArgumentException) { null }` | `s.toIbanOrNull()` |
| `try { IBAN.valueOf(s); true } catch (e: IllegalArgumentException) { false }` | `s.isValidIban()` |

`IbanParseException` extends `IllegalArgumentException`, so the narrowest possible migration from java-iban is to
change nothing but the call itself:

``` kotlin
// java-iban
val iban = IBAN.valueOf(input)

// kiban
val iban = Iban(input)
```

### Typed failures

Catching `IllegalArgumentException` and reading `message` is not necessary. Failures carry a sealed type:

``` kotlin
try {
    Iban(input)
} catch (failure: IbanParseException) {
    when (failure) {
        is IbanParseException.UnknownCountryCode -> failure.countryCode
        is IbanParseException.WrongLength -> "${failure.actualLength} != ${failure.expectedLength}"
        is IbanParseException.WrongChecksum -> "check digits do not match"
        is IbanParseException.Malformed -> failure.kind.name
    }
}
```

## Instance API

| Before | Now |
| --- | --- |
| `iban.toPlainString()` | `iban.plain` |
| `iban.toString()` | `iban.toString()` or `iban.pretty` (unchanged behaviour: spaced formatting) |
| `IBAN.toPretty(input)` | no replacement — parse with `Iban(input)`, then use `iban.pretty` |
| `IBAN.toPlain(input)` | no replacement — parse with `Iban(input)`, then use `iban.plain` |
| `iban.countryCode` | `iban.countryCode` |
| `iban.checkDigits` | `iban.checkDigits` |
| `iban.isSEPA` / `iban.isInSwiftRegistry` | unchanged |

## Bank and branch identifiers

| Before | Now |
| --- | --- |
| `IBANFields.getBankIdentifier(iban)` → `Optional<String>` | `iban.bankIdentifier` → `String?` |
| `IBANFields.getBranchIdentifier(iban)` → `Optional<String>` | `iban.branchIdentifier` → `String?` |
| `CountryCodes.getBankIdentifier(iban)` | `iban.bankIdentifier` |
| `CountryCodes.getBranchIdentifier(iban)` | `iban.branchIdentifier` |

`IBANFields` was JVM-only and returned `java.util.Optional`; it is gone. Kotlin's nullable types cover the same
ground, and `?:` replaces `orElse`.

## CountryCodes

| Before | Now |
| --- | --- |
| `CountryCodes.getLengthForCountryCode(cc)` → `-1` when unknown | `CountryCodes.ibanLength(cc)` → `Int?`, `null` when unknown |
| `CountryCodes.LAST_UPDATE_DATE` / `lastUpdateDateString` | `CountryCodes.lastUpdateDate` → `kotlin.time.Instant` |
| `CountryCodes.LAST_UPDATE_REV` | `CountryCodes.lastUpdateRevision` |
| `CountryCodes.SHORTEST_IBAN_LENGTH` *(kiban 0.5.0)* | `CountryCodes.shortestIbanLength` |
| `CountryCodes.LONGEST_IBAN_LENGTH` *(kiban 0.5.0)* | `CountryCodes.longestIbanLength` |
| `CountryCodes.getLength(cc)` *(kiban 0.5.0)* | `CountryCodes.ibanLength(cc)` |

The three names marked *(kiban 0.5.0)* were renamed in 0.6.0, together with
`Iban.SHORTEST_POSSIBLE_IBAN`, which is now `Iban.SHORTEST_POSSIBLE_IBAN_LENGTH`. All four named a
length without saying so, and the two `CountryCodes` lengths additionally surfaced on the JVM as
`getSHORTEST_IBAN_LENGTH()` / `getLONGEST_IBAN_LENGTH()`.
| `CountryCodes.isKnownCountryCode(cc)` | unchanged |
| `CountryCodes.isSEPACountry(cc)` | unchanged |

## Composition

`Iban.compose` throws on invalid input, same as `Iban(input)`:

``` kotlin
// before
val iban = IBAN.compose("BI", "10000100010000332045181")

// now
val iban = Iban.compose("BI", "10000100010000332045181")
```

If you used `compose` on kiban 0.3.0, note that it was broken for any country whose check digits are 10 or
higher — it produced an IBAN with `00` check digits and then rejected it. Upgrading fixes that; there is no
workaround to remove from your code, since the call simply failed before.

## Modulo97

Unchanged, and still throwing. `Modulo97.checksum`, `calculateCheckDigits`, and `verifyCheckDigits` all take
`CharSequence` and raise `IllegalArgumentException` on malformed input, because their inputs come from the
programmer rather than from an end user.

## Things that were never ported

* `IBANFieldsCompat` — the Java 6/7 compatibility shim for `IBANFields`.
* Any API that took a `java.util.Locale` or returned a `java.util.Optional`.
