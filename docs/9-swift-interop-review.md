# #9 review: is kiban's API good from Swift?

This is the recommendation #9 asked for: evaluate both Swift-facing interop paths for real —
Objective-C interop (today's default) and Swift Export (JetBrains' newer, direct Kotlin→Swift
path) — and decide whether kiban's `Result`-returning API is acceptable from Swift as-is, or
needs a Swift-friendly companion surface, before the 1.0 API freeze.

Everything below was produced by actually compiling and running Swift code against a real
`Kiban.xcframework` / Swift Export output on a `macos-latest` GitHub Actions runner (Kotlin
2.4.10, Xcode 26.6), not inferred from documentation. The testbed lives on the
[`swift-export-playground`](https://github.com/BijdorpStudio/kiban/tree/swift-export-playground)
branch: `samples/swift-console`'s `Probe*` targets (Objective-C interop) and
`samples/swift-export-probe` (Swift Export), driven there by
`.github/workflows/ios-interop-verify.yml`, which anyone can re-run on demand from that
branch (Actions tab → "iOS/Swift interop verification" → "Run workflow"). `main` keeps only
the consumer-facing `SwiftConsole` sample, which the same workflow builds and runs on `main`
as a compile regression check.

## Objective-C interop (today's default framework path)

### `Result<Iban>` erases to a useless, opaque `Any?`

`Iban.parse(input:)` and `Iban.compose(countryCode:bban:)` both return `Result<Iban>` in
Kotlin. From Swift, the declared/inferred return type is plain `Any?` — not `Result`-shaped in
any way. `isSuccess`, `isFailure`, `getOrNull()`, `exceptionOrNull()` do not exist as members;
the compiler error is literally "value of type 'Any?' has no member 'isFailure'".

- **On success**, the `Any?` holds the `Iban` instance directly — recoverable with
  `result as? Iban`.
- **On failure**, the `Any?` holds an instance of Kotlin's own internal, non-public
  `kotlin.Result.Failure` box (confirmed via `String(describing:)`, which prints Kotlin's
  actual `Failure.toString()` output — e.g.
  `Failure(nl.bijdorpstudio.kiban.IbanParseException.WrongChecksum: Wrong check sum for ...)`).
  Since `Result.Failure` is `internal` in the Kotlin stdlib, it isn't exported to Swift as a
  nameable type. `result as? IbanParseException` (and every subtype, including the correctly
  nested `IbanParseException.Malformed`) **fails at runtime for every failure case tested**
  (empty, too short, unknown country, wrong length, wrong checksum). There is no type-safe way
  to reach the exception from Swift through this path — only a free-text description string.

This is worse than the issue's original framing ("no typed access to the failure, awkward
success/failure handling") — there is no programmatic access to the failure at all.

### Top-level extension functions aren't Swift extensions

`toIbanOrNull()`, `isValidIban()`, `toIban()` (declared as `fun String.foo()` in Kotlin) are
not exported as true Swift extensions on `String`. Kotlin/Native's default Objective-C exporter
puts them as static methods on an `IbanKt` facade class instead, with the receiver as the first
parameter: `IbanKt.toIbanOrNull("...")`, not `"...".toIbanOrNull()`. (This was also a real,
previously-unverified bug in the merged `samples/swift-console` sample — the
`ios-interop-verify.yml` workflow that would have caught it had never actually been triggered
before this review.)

### The sealed hierarchy and `Malformed.Kind`

Nested classes keep genuine Swift nested-type names confirmed from the real generated header
(`IbanParseException.Malformed`, `.UnknownCountryCode`, `.WrongLength`, `.WrongChecksum`), and
`Malformed.Kind` is flattened one level to `IbanParseException.MalformedKind`, itself a real,
switchable Swift enum (`CaseIterable`, real `.empty`/`.tooShort`/etc. cases — Swift's compiler
requires `@unknown default:` since it's treated as non-frozen). This actually **contradicts**
the issue's original premise that Objective-C interop wouldn't surface a Kotlin enum as a Swift
enum — apparently that's improved since the issue was filed. It's moot in practice, though: the
`Result` erasure above means a Swift caller can never reach a `Malformed` instance to read
`.kind` from in the first place.

### Undeclared exceptions crash the process

`Iban.valueOf(input:)` (deprecated, calls the non-`@Throws` `Result.getOrThrow()`) crashes the
process with `SIGABRT` when called from Swift with invalid input — confirmed with a full native
crash backtrace through the Kotlin runtime. This matches Kotlin/Native's documented behavior:
an exception crossing into Swift/Objective-C without a declared `@Throws` terminates the app
rather than becoming a catchable Swift error, since `valueOf`/`getOrThrow()` aren't annotated.

## Swift Export (Alpha since Kotlin 2.2, targeted stable during 2026)

Swift Export currently **only works via a real, direct-integration Xcode project build phase**
— there is no standalone Gradle task, no SwiftPM-based way to try it, confirmed both by
official docs and by this review needing to scaffold an actual (XcodeGen-generated) SwiftUI app
target with an `embedSwiftExportForXcode` Run Script phase to get anything real out of it at
all (`samples/swift-export-probe`).

### The API shape is genuinely better — when it works

The real generated `Kiban.swift` (captured from
`library/build/SwiftExport/iosX64/Debug/files/Kiban/Kiban.swift` before the compiler crash
described below) shows `Iban.Companion.parse` returning a real, named
`ExportedKotlinPackages.kotlin.Result` class — not an opaque `Any?`. That class has real,
functional members backed by genuine Kotlin runtime calls:

```swift
public var isFailure: Swift.Bool { get { ... } }
public var isSuccess: Swift.Bool { get { ... } }
public func exceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? { ... }
public func getOrNull() -> (any KotlinRuntimeSupport._KotlinBridgeable)? { ... }
```

`exceptionOrNull()` returns a real `Throwable?` — recoverable and further downcastable to
`IbanParseException` and its real nested subclasses (also present in the generated interface),
unlike the Objective-C path where the equivalent value is permanently locked inside an
unexported internal wrapper. `Malformed.Kind` is again a real `CaseIterable` Swift enum.

This is not a full fix, though: `getOrNull()`'s return type is `any KotlinBridgeable?` (an
existential, not `Iban?`) — Kotlin generics are type-erased to their upper bound under Swift
Export too, so a caller still needs `as? Iban`, just against a reachable value instead of an
unreachable one. Top-level extension functions have the same `IbanKt`-style static-facade
issue as the Objective-C path (`toIbanOrNull` is `IbanKt.toIbanOrNull(_:)`, not a true `String`
extension) — Swift Export didn't fix that either.

### But it isn't reliable today

Actually *compiling* the generated interface failed in this exact experiment:
`compileSwiftExportMainKotlinIosX64` crashed with
`java.lang.NoClassDefFoundError: kotlinx/coroutines/internal/intellij/IntellijCoroutines`, an
internal error inside Kotlin's own Swift Export tooling (its Analysis-API-based `swift-export-
standalone` engine), unrelated to anything in kiban's source. The Kotlin-side interface
*generation* tasks (`iosX64DebugSwiftExport`) succeeded and produced real output on disk; the
following compile step is where it broke. That's a concrete, reproducible confirmation of
Alpha-grade instability, not a hypothetical one.

### It can't reach kiban's actual consumers anyway

Independent of the above: Swift Export's docs state it "currently works only in projects that
use direct integration to connect the iOS framework to the Xcode project" — meaning the
*consuming* Xcode project has to live inside (or reference) the same Gradle project as the
library, regenerating the Swift interface locally on every build via
`./gradlew :library:embedSwiftExportForXcode`. It explicitly doesn't yet support CocoaPods,
Carthage, or (per available documentation) SwiftPM binary distribution.

kiban is the opposite case: it publishes a prebuilt `Kiban.xcframework` to Maven Central for
third-party consumers who never see kiban's Gradle project or source. Even a fully-stabilized
Swift Export would not, as currently designed, change what those consumers get — Swift Export
only helps a first-party app that vendors kiban's *source* inside its own multiplatform
project, which isn't how kiban is used.

## Recommendation

**(b): kiban should add a small Swift-friendly companion surface before 1.0**, rather than
relying on either interop path's current `Result<Iban>` handling:

- The Objective-C path (today's only real option for kiban's actual published-artifact
  consumers) makes `Result<Iban>` failures completely unrecoverable from Swift. That's not
  "awkward", it's broken for that use case.
- Swift Export's `Result` handling is a real, measured improvement, but it's simultaneously
  Alpha-unstable (hit a genuine compiler crash in this review), and structurally unable to
  reach kiban's actual distribution model regardless of stability. Neither gap is something
  kiban's own code can work around.

Concretely, a companion surface should give Swift callers a way to get typed success/failure
information without going through `Result<Iban>` — `toIbanOrNull()` already covers the "don't
care why" case (once callers know to use `IbanKt.toIbanOrNull(_:)`, not `.toIbanOrNull()`); the
gap is the "I do care why it failed" case, which today has no path at all from Swift.

**This is not a dead end for Swift Export** — revisit before/at the point Swift Export leaves
Alpha and, separately, gains support for published-binary consumption rather than only direct
Gradle-project integration. Both are plausible within the 2026 timeframe cited when this issue
was scoped for 0.5.0. Until then, the Objective-C-interop-facing companion surface is the thing
that actually reaches kiban's consumers.

## What to do next (out of scope for this review)

Designing the actual companion surface (naming, whether it lives in `commonMain` behind
`@JvmName`/`@ObjCName` or as an Apple-target-only source set, what shape the error side takes)
is real API design work and its own issue — this review's job was to answer *whether* one is
needed and *why*, with real evidence, not to design it.

## 0.5.0 follow-up: `@Throws` verified for real (2026-08-09)

#100 replaced the `Result<Iban>` shape with strict, `@Throws(IbanParseException::class)`-annotated
parsing. Its own Verification section flagged the Swift catch shape as unconfirmed — that annotated
functions surface to Swift as `throws`, but whether the Kotlin exception arrives usably. Confirmed
on `feature/issue-100-strict-throwing-api` via `ios-interop-verify.yml`, building and running
`samples/swift-console` against a real `Kiban.xcframework`.

- The generated header confirms the documented NSError-out-param shape:
  `invoke(input:)`, `compose(countryCode:bban:)` and the top-level `IbanKt.toIban(...)` each carry
  `error:(NSError * _Nullable * _Nullable)error`, with a Kotlin/Native-generated doc comment on
  every one of them: *"This method converts instances of IbanParseException to errors. Other
  uncaught Kotlin exceptions are fatal."*
- Calling `IbanKt.toIban("not an iban")` from a Swift `do { try ... } catch { ... }` does **not**
  crash the process — unlike the old, unannotated `valueOf`, which the original #9 review confirmed
  SIGABRTs. The caught error prints as:
  ```
  Error Domain=KotlinException Code=0 "Characters at index 2 and 3 not both numeric. notaniban"
  UserInfo={NSLocalizedDescription=Characters at index 2 and 3 not both numeric. notaniban,
  KotlinException=nl.bijdorpstudio.kiban.IbanParseException.Malformed: Characters at index 2 and 3
  not both numeric. notaniban, KotlinExceptionOrigin=}
  ```
  So the original #9 finding — "no programmatic access to the failure at all" — is closed for the
  "does it crash" and "can Swift catch it" questions. **Still open, not exercised by this run**:
  whether `error.userInfo["KotlinException"]` can be `as?`-downcast from Swift to the real
  `IbanParseException.Malformed` type for typed access, versus only reading its string description.
  That's the concrete next thing to verify before calling the companion-surface question (see
  Recommendation, above) fully settled either way.
- `Iban`'s companion `invoke`/`compose` are **not** surfaced as Swift initializers under this
  interop path — they're `Iban.companion.invoke(input:)` / `Iban.companion.compose(countryCode:bban:)`
  (`companion` is a generated static property on the `KibanIban` ObjC class). `operator fun invoke`
  gets no special Swift-init treatment here, which is why `samples/swift-console` calls the
  top-level `IbanKt.toIban(...)` for its throwing-path demo instead of guessing at `Iban(...)`
  syntax.
