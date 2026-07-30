# Migration guide

Kiban is a port of [`java-iban`](https://github.com/barend/java-iban). This document maps the old API onto the
current one, both for people arriving from `java-iban` and for people upgrading from kiban 0.3.0 or earlier.

Most of the work is mechanical. The deprecated declarations carry `ReplaceWith`, so the IDE can apply the
majority of these changes for you: **Code > Inspect Code**, or Alt+Enter on each warning. The compat layer stays
in place until 1.0.

## Package and type names

| java-iban | kiban |
| --- | --- |
| `nl.garvelink.iban` | `nl.bijdorpstudio.kiban` |
| `IBAN` | `Iban` (a deprecated `typealias IBAN = Iban` exists on JVM) |
| `Modulo97` | `Modulo97` |
| `CountryCodes` | `CountryCodes` |
| `IBANFields` | removed — use `Iban.bankIdentifier` / `Iban.branchIdentifier` |
| `IBANFieldsCompat` | never ported |

## Parsing

The big change. `Iban.parse` returns `Result<Iban>` instead of throwing.

| Before | Now |
| --- | --- |
| `IBAN.valueOf(input)` | `Iban.parse(input).getOrThrow()` — or better, handle the `Result` |
| `IBAN.parse(input)` | `Iban.parse(input)`, which now returns `Result<Iban>` |
| `try { IBAN.valueOf(s) } catch (e: IllegalArgumentException) { null }` | `s.toIbanOrNull()` |
| `try { IBAN.valueOf(s); true } catch (e: IllegalArgumentException) { false }` | `s.isValidIban()` |

`getOrThrow()` throws the same `IllegalArgumentException` the old API threw, so the narrowest possible migration
is to append it to every call and change nothing else:

``` kotlin
// java-iban
val iban = IBAN.valueOf(input)

// kiban, minimal change
val iban = Iban.parse(input).getOrThrow()

// kiban, idiomatic
Iban.parse(input).fold(
    onSuccess = { accept(it) },
    onFailure = { showError(it.message) }
)
```

### Typed failures

Catching `IllegalArgumentException` and reading `message` is no longer necessary. Failures carry a sealed type:

``` kotlin
when (val failure = Iban.parse(input).exceptionOrNull()) {
    is IbanParseException.UnknownCountryCode -> failure.countryCode
    is IbanParseException.WrongLength -> "${failure.actualLength} != ${failure.expectedLength}"
    is IbanParseException.WrongChecksum -> "check digits do not match"
    is IbanParseException.Malformed -> failure.kind.name
    null -> "parsed successfully"
}
```

`IbanParseException` extends `IllegalArgumentException`, so existing `catch` blocks around `getOrThrow()` keep
working unchanged.

## Instance API

| Before | Now |
| --- | --- |
| `iban.toPlainString()` | `iban.plain` |
| `iban.toString()` | `iban.toString()` or `iban.pretty` (unchanged behaviour: spaced formatting) |
| `IBAN.toPretty(input)` | `Iban.format(input)` |
| `IBAN.toPlain(input)` | `Iban.toPlain(input)` |
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
| `CountryCodes.getLengthForCountryCode(cc)` → `-1` when unknown | `CountryCodes.getLength(cc)` → `Int?`, `null` when unknown |
| `CountryCodes.LAST_UPDATE_DATE` / `lastUpdateDateString` | `CountryCodes.lastUpdateDate` → `kotlin.time.Instant` |
| `CountryCodes.LAST_UPDATE_REV` | `CountryCodes.lastUpdateRevision` |
| `CountryCodes.isKnownCountryCode(cc)` | unchanged |
| `CountryCodes.isSEPACountry(cc)` | unchanged |

## Composition

`Iban.compose` also returns a `Result`:

``` kotlin
// before
val iban = IBAN.compose("BI", "10000100010000332045181")

// now
val iban = Iban.compose("BI", "10000100010000332045181").getOrThrow()
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
