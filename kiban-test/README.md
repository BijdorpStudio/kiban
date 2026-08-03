# kiban-test

The home for test helpers that kiban's consumers want but the main `kiban` artifact
deliberately doesn't ship — starting with a per-country `Iban.random()` (tracked
separately, see below). This module exists so those helpers don't widen the public
API surface `apiCheck` guards on the main artifact, or ship generator code to
production classpaths.

## Why a separate module, not test fixtures

Gradle's `java-test-fixtures` plugin — a fixtures capability of the *same* module,
published alongside the main artifact, consumed with `testFixtures(...)` — is the
obvious first choice, and is exactly what AGP offers Android libraries
(`android.testFixtures`). It doesn't work here: `java-test-fixtures` is JVM-only and
does not compose with the `kotlin("multiplatform")` plugin's source-set model, and
first-class KMP test fixtures aren't shipped yet — [KT-63142](https://youtrack.jetbrains.com/issue/KT-63142)
is still open, with no indication of it landing in a released Kotlin version. Other
multiplatform libraries that hit this (e.g. Slack's
[EitherNet](https://github.com/slackhq/EitherNet)) worked around it the same way
this module does: a separate published module that depends on the main artifact,
rather than a fixtures capability of it.

Given that, a JVM/Android-only fixtures module was the other option on the table,
but kiban runs its test suite on 19 targets; helpers that vanish on native and JS
targets would defeat the point for a KMP library. So `kiban-test` mirrors `:library`'s
full target matrix.

`org.jetbrains.kotlinx.binary-compatibility-validator` needs no extra wiring for
this: applied once at the root, it validates every subproject with its own
`api/` dump, so `kiban-test` gets a dump and `apiCheck` independent of `:library`'s.

## Scope of this module today

This is the scaffold only — `commonMain` depends on `:library` (as `api`, so
consumers of `kiban-test` get `Iban` and friends transitively, mirroring how a
`testFixtures` consumer gets the main module's classes for free) but has no test
helpers yet. `Iban.random()` itself is a separate ticket now that this home exists.
