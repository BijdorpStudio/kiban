# Is `kotlin.time.Instant` safe to freeze in the public API?

Issue: [#144](https://github.com/BijdorpStudio/kiban/issues/144)

`CountryCodes.lastUpdateDate` is a `kotlin.time.Instant`. Before 1.0 freezes it,
#144 asked for confirmation that the type is stable — not experimental — at the
Kotlin 2.3.0 language/API compatibility level that `tapmoc` pins
(`library/build.gradle.kts`, `gradle/libs.versions.toml`), because an
experimental stdlib type in a frozen public API would be a trap for consumers.

**Conclusion: yes, keep it.** The type is stable from Kotlin 2.3, which is
exactly the floor kiban already declares. The caveat is that the floor is
load-bearing for this decision in a way it was not before, so it is now written
down (see [What this constrains](#what-this-constrains)).

## Evidence

### 1. The stdlib declaration is stable, and stable *as of 2.3*

From `kotlin-stdlib-2.4.10.jar`, `kotlin/time/Instant.class`:

```
RuntimeInvisibleAnnotations:
  kotlin.SinceKotlin(version="2.3")
  kotlin.WasExperimental(markerClass=[class Lkotlin/time/ExperimentalTime;])
```

Three things matter here:

* `@ExperimentalTime` is **not** applied to the class. `Instant` is not an
  experimental declaration.
* `@SinceKotlin("2.3")` sets the API version from which it is available as a
  stable declaration.
* `@WasExperimental(ExperimentalTime::class)` is what makes the boundary sharp:
  below API version 2.3 the compiler treats the declaration as still-experimental
  and demands opt-in; at 2.3 and above it does not.

The companion members in use (`Instant.parse`, and `DISTANT_PAST` in tests)
carry no separate experimental markers of their own.

### 2. The library compiles at 2.3 with no opt-in anywhere

Effective options on `:library:compileKotlinJvm`:

```
apiVersion=KOTLIN_2_3 languageVersion=KOTLIN_2_3 optIn=[]
freeArgs=[-Xexplicit-api=strict, ..., -Xjdk-release=17]
```

`optIn` is empty and the module compiles clean under `-Xexplicit-api=strict`.
No `@OptIn(ExperimentalTime::class)` exists anywhere in the source tree. If
`Instant` were experimental at this level, this build would not pass.

### 3. The boundary is real, and it is exactly 2.3

Dropping the pin to 2.2 (`tapmoc { kotlin("2.2.0") }`) and rebuilding fails:

```
apiVersion=KOTLIN_2_2 languageVersion=KOTLIN_2_2 optIn=[]
e: CountryCodes.kt:166:32 This declaration needs opt-in. Its usage must be marked
   with '@kotlin.time.ExperimentalTime' or '@OptIn(kotlin.time.ExperimentalTime::class)'
e: CountryCodes.kt:167:17 ...
e: CountryCodes.kt:167:25 ...
```

So the stability of this API surface rests entirely on the 2.3.0 floor. That is
a confirmation, not a problem — but it is worth being precise about who else it
binds.

### 4. It binds consumers, not just this build

A consumer compiling against kiban at API version 2.2 hits the same wall — the
opt-in requirement propagates through the return type. Compiling a probe in
`:samples:jvm-cli` with `apiVersion = 2.2`:

```kotlin
fun probe(): String = CountryCodes.lastUpdateDate.toString()
```

```
e: Probe.kt:5:36 This declaration needs opt-in. Its usage must be marked with
   '@kotlin.time.ExperimentalTime' or '@OptIn(kotlin.time.ExperimentalTime::class)'
```

This is the "trap for consumers on older compiler settings" #144 anticipated —
and it is confined to `lastUpdateDate`. Depending on kiban, parsing IBANs, and
every other API is unaffected; only this one property requires the caller to be
at 2.3 or to opt in.

Such a consumer is already outside kiban's declared compatibility range, so this
is not a defect. It does mean the Kotlin floor cannot quietly drop below 2.3
while this property is in the API.

## Decision

Keep `lastUpdateDate: kotlin.time.Instant` and freeze it.

The alternative #144 floated — exposing the ISO-8601 date as a `String`
alongside or instead — is rejected:

* The premise for it was doubt about stability. There is no doubt: the evidence
  above is unambiguous.
* `lastUpdateDateString` was *removed* in favour of the typed property during
  the java-iban migration (see MIGRATION.md and CHANGELOG.md). Re-adding a
  stringly-typed twin would undo a deliberate decision and leave two ways to
  read one value in the frozen API — the more expensive outcome of the two, and
  permanently so.
* A consumer who wants the ISO-8601 form has it: `lastUpdateDate.toString()`
  renders `yyyy-mm-ddT00:00:00Z`, which is now a tested part of the contract.

## What this constrains

Recorded so the reasoning is not lost the next time the toolchain moves:

* **The Kotlin compatibility floor cannot go below 2.3.0** while `lastUpdateDate`
  is in the public API. Lowering `kotlin-version` in
  `gradle/libs.versions.toml` breaks the library build loudly (evidence 3), so
  this is self-enforcing rather than a silent regression — but the failure would
  look like an unrelated opt-in error, hence this note and the pointer at the
  declaration.
* **The midnight-UTC encoding is part of the contract**, not an implementation
  detail of the getter. A registry release is dated to the day; with no
  `LocalDate` in the standard library and a zero-dependency constraint that rules
  out `kotlinx-datetime`, the date is carried as the instant at `00:00:00Z`.
  `CountryCodesTest` now pins that: zero nanoseconds, an epoch-second count on a
  day boundary, and an ISO-8601 rendering that round-trips.
* Callers should read the *date* off the value. Reading a local calendar date
  from it in a non-UTC zone can land a day off, which is inherent to encoding a
  date as an instant and is documented on the property.

## Reproducing

```bash
# stdlib annotations
unzip -p ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/*/*/kotlin-stdlib-*.jar \
  kotlin/time/Instant.class > /tmp/Instant.class
javap -v -p /tmp/Instant.class | grep -A4 '^RuntimeInvisibleAnnotations'

# the library builds clean at the pinned level
./gradlew jvmTest apiCheck
```
