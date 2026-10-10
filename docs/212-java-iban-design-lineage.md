# #212: Where kiban's design decisions come from

[README.md](../README.md#inherited-from-java-iban) lists the design decisions kiban inherited from
[`java-iban`](https://github.com/barend/java-iban) as the project's own, because that is what they
are: every one of them still holds, and a consumer reading the README needs the rule, not its
history. This document keeps the history, which #212 moved out of the README — it was written in
Barend Garvelink's first person about an Android app that no longer sets the constraints.

## The original context

java-iban started as a utility inside an Android app of the SDK 14 (Ice Cream Sandwich) era, on
Java 1.6, and its author set out to follow the design principles of
[Joda-Time](https://www.joda.org/joda-time/) — a small, immutable value type with no configuration
surface, in preference to a pluggable one. Two of the decisions the README carries are that context
rather than general principle:

* **No format-mask enforcement.** The national format mask (such as `QA2!n4!a21!c`) was left
  unenforced in part to keep a regex engine out of an Android app's parse path. Barend also noted
  the mask's one genuinely Android-shaped use, keyboard switching on an IBAN `EditText`, as work for
  a different project.
* **Bit-packed registry data.** The country table is packed into `Int`s rather than held as objects
  or parsed strings because bytecode size and allocation count were what mattered on an SDK 14
  device.

## What that means for kiban

The decisions stand, but not all of the reasoning transfers, so it is worth being explicit about
which is which.

The format masks and the national check digits stay unenforced on their own merits: the modulo-97
checksum catches most input errors already, a regex per parse is a cost every target pays and not
just Android, and national check digits differ per bank identifier and need country-specific
knowledge this project does not claim. kiban also now has `minSdk` 24 rather than SDK 14, so the
Android floor is no longer the binding constraint on any of it — the argument is about what
validation the library is willing to promise, not about what an old phone can afford.

The bit-packing stays for a reason the original did not have: kiban ships to every Kotlin target,
including Kotlin/JS and Kotlin/Wasm, where the generated data is downloaded rather than installed.
It is explicitly not part of the API contract — [VERSIONING.md](../VERSIONING.md) names
`CountryCodesData` and the packing behind it as internals that may change in any release — so it
can be revisited whenever the measurements say so, without a deprecation cycle.

Attribution to Barend Garvelink is unchanged by the rewrite: it stays in the per-file license
headers and in the README's introduction and Background section.
