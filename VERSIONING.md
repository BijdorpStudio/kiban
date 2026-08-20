# Versioning, compatibility and deprecation policy

Kiban follows [Semantic Versioning 2.0.0](https://semver.org). This document says what a version
bump means for a consumer, what "compatible" covers and what it does not, how the guarantee is
enforced rather than merely promised, and what a removal looks like once the API is frozen.

**Current status: pre-1.0.** The guarantees below describe the 1.0 contract. Until 1.0 is tagged
the API is still being settled: the 0.x line breaks compatibility where the freeze would otherwise
lock in a mistake, and removals land without a deprecation cycle (see
[MIGRATION.md](MIGRATION.md) for what has already moved). Everything else here — how the API
surface is defined, how it is checked, what is deliberately outside the contract — is already how
the repository works today.

## What a version bump means

| Bump | Means |
| --- | --- |
| **Major** (`1.0.0` → `2.0.0`) | The public API changed incompatibly: a declaration was removed or its signature narrowed, previously accepted input is now rejected, a target was dropped, or a toolchain floor rose. Upgrading may require code changes, and code compiled against the previous major may not link. |
| **Minor** (`1.0.0` → `1.1.0`) | Additive and backwards compatible: new declarations, new targets, new IBAN registry data, deprecations. Code written against the previous minor compiles and links unchanged. |
| **Patch** (`1.0.0` → `1.0.1`) | No API change at all. Fixes to behaviour that was already wrong against its own documentation, performance work, documentation and build changes. |

Two consequences worth stating outright, because they are the cases that usually get fudged:

* **A SWIFT registry data update is a minor, never a patch.** Adding a country, changing an IBAN
  length or moving a bank identifier's offsets changes what `Iban(...)` accepts and what
  `bankIdentifier` returns. That is new behaviour, not a fix, even though the source diff is
  confined to generated data.
* **Tightening validation is a major.** Rejecting input that a previous release accepted breaks
  callers who were relying on it, whatever the standard says. This is why the 0.6.0 line spends
  its breaking-change budget on exactly this class of change (ASCII-only input, whitespace
  leniency narrowed) before the freeze rather than after it.

## What the public API is

The public API is what the binary compatibility validator dumps, and nothing else:

* `library/api/jvm/library.api` — the JVM/Android ABI.
* `library/api/library.klib.api` — the klib ABI, covering every Kotlin/Native, JS and Wasm target.

The library is built with `explicitApi()` (`library/build.gradle.kts`), so every public declaration
has to state its visibility and return type deliberately; nothing reaches the frozen surface by
omission. Anything `internal` is not part of the contract even where a platform makes it
technically reachable.

Three things are deliberately **not** contract, and may change in any release:

* **Exception messages.** `IbanParseException` and its subclasses carry typed data — `Malformed.kind`,
  the character and index that caused a rejection — precisely so that no caller has to match on
  message text. The messages are diagnostics.
* **Iteration and collection order that is not documented.** Where order is part of the contract it
  is stated in the KDoc — `CountryCodes.knownCountryCodes` is alphabetical, and its `List` return
  type says so.
* **Generated data internals.** `CountryCodesData` and the bit-packing behind it are an
  implementation detail of the lookup API.

## Binary compatibility, and how it is enforced

Within a major version, an artifact built against `1.x` links against any later `1.y` (`y >= x`) on
every published target. That is checked mechanically, not reviewed by eye:

* `apiCheck` (wired in the root `build.gradle.kts` with `klib { enabled = true }`) diffs the current
  API surface against the committed dumps and fails the build on any divergence.
* It runs on every pull request as its own CI job. Because building the klib dump compiles every
  target the host supports, that job doubles as the compile check for the Apple targets.
* An intentional API change is not an override or a suppression: run `./gradlew apiDump` and commit
  the regenerated dumps. The API delta then shows up in the pull request diff as reviewable lines,
  which is the point — nothing changes the frozen surface without a reviewer seeing the exact
  change.

Source compatibility is the stronger of the two and is what the table above promises: within a
major version, source that compiled against an earlier minor still compiles. Note that the reverse
of a common assumption holds on the JVM — narrowing a return type is source-compatible but
binary-breaking (`CountryCodes.knownCountryCodes` moving from `Collection<String>` to `List<String>`
changes the `getKnownCountryCodes` descriptor), which is exactly why the dumps, not the compiler,
are the arbiter.

## Deprecation policy

From 1.0 onwards, nothing public is removed without warning. A removal runs this cycle:

1. **Announce.** In a minor release the declaration is annotated
   `@Deprecated(message, ReplaceWith(...), level = DeprecationLevel.WARNING)`. `ReplaceWith` is
   supplied whenever the replacement is mechanical; when it is not, the message names the
   replacement in prose and the CHANGELOG entry shows the rewrite. The declaration keeps working.
2. **Escalate.** No earlier than the next minor release, the level may be raised to
   `DeprecationLevel.ERROR`. Existing binaries still link — the declaration is still there — but new
   code cannot compile against it.
3. **Remove.** Only in the next major release.

So the floor is: a declaration deprecated in `1.x` survives every remaining `1.y`, and disappears at
`2.0` at the earliest — never fewer than one full minor release after the warning first shipped, and
never inside a major version.

Deprecations are additive and therefore ship in minor releases; they appear in the API dumps, so
both the deprecation and the eventual removal are visible in a reviewable diff.

## Adding a parameter to a published function

Adding a parameter with a default value looks source-compatible and is not binary-compatible: the
signature in the compiled artifact changes, so a caller compiled against the old one fails with
`NoSuchMethodError` until it is recompiled. It is the one everyday API-evolution move that silently
breaks consumers, which is why it has a rule of its own.

Annotate the new parameter with the kiban version that introduced it:

``` kotlin
@OptIn(ExperimentalVersionOverloading::class)
public fun example(input: String, @IntroducedAt("1.1") pretty: Boolean = false): String
```

The compiler then emits the older signature as a hidden overload, so binaries built against `1.0`
keep linking. Both shapes show up in the dumps —

```
public static final synthetic fun example (Ljava/lang/String;)Ljava/lang/String;
public static final fun example (Ljava/lang/String;Z)Ljava/lang/String;
```

— which means the compatibility shim is reviewed like any other API change rather than being taken
on trust. The version string is the kiban release the parameter appears in, not a Kotlin version.

Two caveats worth knowing rather than discovering:

* This is why the Kotlin floor is 2.4.0. Below a 2.4 `languageVersion` neither annotation resolves.
* `ExperimentalVersionOverloading` is `@RequiresOptIn(level = ERROR)`, so this is an experimental
  compiler feature carried inside an artifact whose API is frozen. If it changes shape, the fallback
  is the pre-2.4 approach — declare the new arity as a separate overload and keep the old signature
  as a `@Deprecated(level = DeprecationLevel.HIDDEN)` one — which is what the annotation automates,
  not something it makes newly possible.

Adding an optional parameter this way is additive, so it ships in a minor release.

## Consumer requirements

These are part of the compatibility contract. Raising any of them breaks consumers who cannot
follow, so it happens **only in a major release**.

| Requirement | Value |
| --- | --- |
| Kotlin (consumer compiler and `apiVersion`) | **2.4.0** or newer |
| Java bytecode / `-Xjdk-release` | **17** |
| Android `minSdk` | **24** |
| macOS target | **`macosArm64` only** — no Intel slice since 0.5.0 |

The Kotlin and Java floors are pinned by the `tapmoc` plugin from single entries in
`gradle/libs.versions.toml` (`kotlin-version`, `java-version`), so they cannot drift by accident
when the compiler used to *build* the library moves forward — which it does independently, and
which is not itself a breaking change.

The Kotlin floor is load-bearing rather than incidental, and two separate things hold it up:

* `@IntroducedAt` and `ExperimentalVersionOverloading` do not resolve below a 2.4 `languageVersion`,
  and they are what makes an optional parameter addable to a published function without a break —
  see [Adding a parameter to a published function](#adding-a-parameter-to-a-published-function).
* `CountryCodes.lastUpdateDate` returns `kotlin.time.Instant`, which the standard library only makes
  non-experimental from 2.3. That is subsumed by the 2.4 floor, but it is why the floor can never go
  *below* 2.3 — a lower one turns a frozen public API into one that demands
  `@OptIn(kotlin.time.ExperimentalTime::class)` from callers. See
  [docs/144-instant-api-stability.md](docs/144-instant-api-stability.md).

The floor moved from 2.3.0 to 2.4.0 deliberately before the freeze rather than after it: pre-1.0 the
break costs a minor, and from 1.0 the rule above would make the same move cost a major and strand
the 1.x line without `@IntroducedAt` for its whole life.

## Targets

The published target set is part of the contract. Adding a target is additive and ships in a minor
release. **Removing one is a major change**, including where the removal is forced from outside —
`macosX64`, `tvosX64` and `watchosX64` went away in 0.5.0 because JetBrains deprecated them in
Kotlin 2.3.20, and that was still a break for consumers on Intel Macs. Pre-1.0 it cost a minor;
after 1.0 the same event costs a major.

The `js` and `wasmJs` artifacts serve Kotlin/JS and Kotlin/Wasm consumers. Nothing is annotated
`@JsExport`, so no JavaScript- or TypeScript-facing surface exists and none is frozen by 1.0.
Adding one later stays possible as a purely additive change; withdrawing it again would not be.

## Where things are recorded

* [CHANGELOG.md](CHANGELOG.md) — every release, with breaking changes called out first.
* [MIGRATION.md](MIGRATION.md) — the mapping for each break, from `java-iban` onwards.
* [RELEASING.md](RELEASING.md) — the mechanical checklist for cutting and publishing a release.
* The API dumps under `library/api/` — the surface itself, versioned in git.
