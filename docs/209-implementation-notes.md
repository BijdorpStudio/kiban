# Implementation notes

Issue: [#209](https://github.com/BijdorpStudio/kiban/issues/209)

Why the internals do things the slightly unusual way, where the signature does not say. None of this
is visible to a consumer — [209-api-design-notes.md](209-api-design-notes.md) covers the public
surface. The declarations carry a one-line pointer here where the choice is not obvious.

Parsing is the hot path: `Iban.validate` runs on every `toIban`, `toIbanOrNull` and `isValidIban`
call, and most of what follows is about not allocating on it.

## `Iban.pretty` is eager, not `by lazy`

The `Lazy` instance and the published field behind it cost more memory than the at most
34-character string they would defer. An `Iban` is an immutable value shared freely between threads,
so a lazy here would have to be the thread-safe kind and pay for a volatile read on every
`toString` as well.

## `Iban.countryIndex` is resolved once in the initializer

Every country-dependent value on an `Iban` — `isSepa`, `isInSwiftRegistry`, `bankIdentifier`,
`branchIdentifier` — is a lookup into the same reference-data row, and the binary search that finds
the row is the expensive part. `CountryCodes.bankIdentifierAt` and friends therefore take the
already-resolved index rather than a country code.

Construction is only reachable through `validate`, which rejects an unknown country code, so the
index is negative only for an `Iban` that cannot exist.

## `Iban.ofValidated` and the top-level extensions

`toIban` and `toIbanOrNull` must call `Iban.ofValidated(...)`, not `Iban(...)`. They are top-level
functions, so `Iban(...)` there resolves to `Iban.Companion.invoke` and re-runs `validate` on an
input already known to be valid.

## `validate` scans for an invalid character only after the length check

A character the IBAN character set does not contain — a tab, a non-breaking space, a `$` — adds to
the length just like a legitimate character does, so the length is only the interesting failure once
the characters are known to be legitimate. Correct-length input carrying such a character is caught
further down by `Modulo97`, and without the scan the two paths would blame different things for the
same mistake.

The scan is deliberately not hoisted above the length comparison: it is wasted work for the valid
input that is the hot path.

`Modulo97.checksum` can only throw here for a character outside the IBAN character set, which the
same scan names. Its other rejection — fewer than five non-space characters — cannot happen at that
point, because the length has already been matched against the country's registered length.

## `isKnownCountryCodeInWrongCase` returns early twice

It is only called once the code has already failed the `CountryCodes.ibanLength` lookup. A
non-letter cannot be a country code at all, and an all-upper-case code that failed the lookup is
genuinely unknown — upper-casing it cannot change the outcome. Both early returns keep a doomed
input from paying for a second binary search.

## `toPlain` scans for a space before filtering

A stored IBAN usually holds no space at all, so the scan comes first: finding none means the input
is already plain and only has to be materialized as a `String`, which costs nothing at all when it
is one.

It is deliberately not `Char.isWhitespace`: only the space `addSpaces` emits is grouping. Every
other whitespace character is a character an IBAN cannot contain, and is left in place for
`validate` to reject.

## `addSpaces` writes into a pre-sized `StringBuilder`

Rather than `chunked(4).joinToString(" ")`, which allocates a list and a string per group of four —
on a path every `Iban` construction now runs.

## The ASCII character helpers

Bit 5 of an ASCII letter is its case bit: setting it maps `A`-`Z` onto `a`-`z` and clearing it maps
them back. Testing a letter of either case is therefore one range check rather than two, and
upper-casing one is a single mask rather than a trip through the Unicode case tables
`Char.uppercaseChar` consults. `uppercaseAscii` relies on its receiver already being an ASCII
letter: every other character comes back mangled, not unchanged.

`isAsciiDigit`, `isAsciiLetter` and `isAsciiLetterOrDigit` exist instead of `Char.isDigit`,
`Char.isLetter` and `Char.isLetterOrDigit` because the standard library ones are Unicode-aware on
every platform and accept fullwidth (`９`), Arabic-Indic (`٩`) and other non-ASCII characters that
ISO 13616 does not allow — see
[209-api-design-notes.md](209-api-design-notes.md#ascii-only-and-reject-rather-than-normalize). The
same reasoning applies to the digit branch in `Modulo97.fold`.

## `CountryCodes.indexOf` searches the array directly

Rather than through `asList().binarySearch(..)`: every lookup on the parse path goes through here,
and the list wrapper is an allocation per call that buys nothing.

## `CountryCodes.lastUpdateDate` is parsed once

The encoded date is a compile-time constant, so re-parsing it on every read would buy nothing. It is
parsed when the `CountryCodes` object initializes.

## `Modulo97.fold` keeps a running remainder

Folding each digit in as `remainder = (remainder * 10 + digit) % 97` is the same arithmetic as
reading the whole transformed string as one number and taking it modulo 97, but it needs no buffer
to hold that string: a letter expands into its two digits in place. The remainder never leaves
`0..<97`, so no intermediate exceeds `97 * 10 + 9` and the whole fold fits in an `Int`. A letter's
numeric value is always in `10..35`, the range ISO 13616 assigns to `A`-`Z`, so `foldLetter` always
contributes exactly two digits.

The consequence for a caller: `checksum` allocates nothing beyond what it was passed, and has no
input size beyond which it breaks down.

## `atLeastFiveNonSpaceCharacters` counts rather than filters

It runs on every checksum, and the count is all the caller needs — no intermediate string.
