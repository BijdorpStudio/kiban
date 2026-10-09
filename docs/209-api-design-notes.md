# Public API design notes

Issue: [#209](https://github.com/BijdorpStudio/kiban/issues/209)

Why the public surface is shaped the way it is. The declarations themselves carry a contract
description and, where a choice is not obvious from the signature, a one-line pointer here.
[VERSIONING.md](../VERSIONING.md) is the policy this all has to live inside; this page is the
reasoning behind individual decisions.

## `String` receivers on the extension functions

`String.toIban()`, `String.toIbanOrNull()` and `String.isValidIban()` take a `String` receiver,
while `Iban.invoke`, `Iban.parse`, `Iban.compose`, `Modulo97` and `CountryCodes` all accept any
`CharSequence`. The asymmetry is the decision, not an oversight.

* The three extensions are the Kotlin-idiomatic sugar, and Kotlin's own conversion extensions —
  `toInt()`, `toLong()`, `toBoolean()` — are declared on `String` too. Reading
  `"NL91ABNA0417164300".toIban()` as a peer of `"42".toInt()` is the whole point of their
  existence.
* A `String` receiver exports as an `NSString *` parameter of the generated `IbanKt` facade, so
  `IbanKt.toIban(_:)` stays type-checked at the Swift call site. A `CharSequence` receiver has no
  Objective-C counterpart and erases to an untyped `id`, which moves the mistake of passing the
  wrong thing from compile time to a runtime cast failure.

Callers holding something else — a `StringBuilder`, an Android `Editable`, a slice of a larger
buffer — are not shut out: `Iban.invoke` and `Iban.parse` take a `CharSequence` directly and parse
identically. The exception-free pair has no `CharSequence` counterpart, so
`builder.toString().toIbanOrNull()` is the way there — one copy of an input that validation
materializes into a `String` internally regardless.

## `Iban(input)` and `Iban.parse(input)`

`Iban.parse` is identical to `Iban.invoke` in every way. It exists because `invoke` has no call
syntax outside Kotlin: `Iban(input)` reads as `Iban.Companion.invoke(...)` from Java and
`Iban.companion.invoke(input:)` from Swift, neither of which is an API anyone would write on
purpose. Kotlin callers should keep using `Iban(input)`.

The name follows the `java-iban` heritage this library continues, and reads naturally next to
`String.toIban` and `String.toIbanOrNull`.

## The `IBAN` typealias is JVM-only and kept for 1.0

`IBAN` aliases `Iban` under the class name the original `java-iban` library used, for callers
migrating from it.

It is declared in `jvmMain`, so a `commonMain` consumer — and every non-JVM target — sees only
`Iban`. That is deliberate: `java-iban` is a JVM library, so a caller migrating from it is by
definition compiling for the JVM, and the only way to make the name visible in common code would be
an `expect`/`actual` pair that puts a spelling convenience into the multiplatform API surface no
multiplatform caller asked for.

It is kept for 1.0. A typealias is erased at compile time — no class file is generated, nothing
appears in the JVM API dump, and there is no second type for `Iban` to stay compatible with — so
carrying it costs nothing, while removing it would break the source compatibility of exactly the
migrating callers it exists for. New code should write `Iban`; `IBAN` is there so that a `java-iban`
port compiles with its import changed and nothing else.

## ASCII only, and reject rather than normalize

ISO 13616 defines the IBAN character set as `A-Z0-9`, so non-ASCII look-alikes — fullwidth `９`,
Arabic-Indic `٩` — are rejected rather than normalized. Callers whose input layer can produce them
should normalize (NFKC) before parsing.

The only whitespace tolerated is the (ASCII 0x20) space, and only between the first and last
character: a leading or trailing space is rejected, as is any other whitespace anywhere, tabs and
non-breaking spaces included. Callers whose input layer can produce them should trim before
parsing.

A known country code written in lower case stays rejected for the same reason — kiban rejects rather
than normalizes — but it gets its own `Kind.NonUpperCaseCountryCode` rather than
`UnknownCountryCode`, because the country *is* known and the diagnosis should say so.

## `Malformed.Kind` is a sealed hierarchy, not an enum

The problems that can name the offending character, or the reason behind them, carry it as typed
data, so a caller can react to a rejection instead of parsing the message for it. A `when` over the
sealed type is still exhaustive without an `else`, as it was over the enum.

Like the exceptions that carry them, kinds are constructed by kiban only: the subtypes that carry
data have internal constructors, and no `copy` or destructuring, so a caller cannot build a kind the
parser never produced. The same holds for `IbanParseException` itself — the subtypes exist to be
caught and inspected, not to be created, which keeps the message wording of a rejection the
library's to decide.

`malformedMessage` is total over the hierarchy: a kind either has wording of its own or carries the
detail its wording needs, so there is no way to build a malformed rejection that cannot describe
itself. Under the enum that was a runtime invariant, enforced only once a rejection was turned into
an exception; the type system now holds it at the point of construction.

## `IbanParseException.input`

The offending input is carried with the (ASCII 0x20) spaces that group a pretty-printed IBAN
removed. Nothing else is stripped: any other whitespace the input carried is preserved, because it
is a character an IBAN cannot contain and is often the very reason for the rejection.

## `CountryCodes.lastUpdateDate` is an `Instant` at midnight UTC

The SWIFT IBAN Registry dates its releases to the day, so the value is a date, not a moment. With no
`LocalDate` in the standard library and a zero-dependency constraint that rules out
`kotlinx-datetime`, it is encoded as the `Instant` at midnight UTC on that date.

That encoding is part of the contract: the returned instant always has a zero time-of-day component
and renders as `yyyy-mm-ddT00:00:00Z`. Read the date off it, not the time of day, and do not read a
local calendar date off it in a non-UTC zone.

`Instant` is safe to depend on here: it is a stable, non-experimental standard library type from
Kotlin 2.3 onwards, and this library's minimum supported Kotlin version is 2.4. See
[144-instant-api-stability.md](144-instant-api-stability.md) for the analysis behind freezing it
into the API.

## `CountryCodes.knownCountryCodes` is a defensive copy

The list is an immutable copy of the library's reference data: it rejects every mutation attempt
with an `UnsupportedOperationException`, including through a cast to `MutableList`.

## `Modulo97.checksum` is the raw primitive

It is expected but not enforced that the characters at index 2 and 3 are numeric, and the return
value means different things depending on what is there — `98 - result` gives the check digits when
they are `00`, and a correct input checksums to `1` otherwise. `calculateCheckDigits` and
`verifyCheckDigits` are the two shapes callers normally want; `checksum` is public because both of
those are built on it and a caller verifying something other than an IBAN needs it.
