# Samples

Consumer projects for `kiban`, focused on API/interop ergonomics rather than Maven packaging
(the publish flow, `apiCheck`, and `verifyPublicationTargets` already cover the packaging side).

`swift-console` and `swift-export-probe` are the testbed for
[`docs/9-swift-interop-review.md`](../docs/9-swift-interop-review.md) — read that first for
the actual findings; the notes below are about running the samples themselves.

## jvm-cli

A runnable walkthrough of the API, depending on `:library` directly and following README.md's
"Use" section step by step, printing each result. Not a test suite — running it is the check.

```shell
./gradlew :samples:jvm-cli:run
```

## swift-console

A Swift Package Manager executable exercising the library through Kotlin/Native's
Objective-C interop — the concrete testbed #9 needs to evaluate that for real. Needs a macOS
host with Xcode; see `.github/workflows/ios-interop-verify.yml` for on-demand CI access to one.

The package declares several executables (product names match the target names below —
there's no `products:` array, SwiftPM vends one implicitly per target):

- `SwiftConsole` — the happy-path walkthrough (parsing, formatting, `isSEPA`, etc). Top-level
  Kotlin extension functions (`toIbanOrNull`, `isValidIban`) are NOT exported as true Swift
  extensions on `String` — Kotlin/Native's default ObjC exporter puts them as static methods
  on an `IbanKt` facade class instead (confirmed from the real generated header: the receiver
  is the function's first parameter), so this calls `IbanKt.toIbanOrNull("...")`, not
  `"...".toIbanOrNull()`.
- `ProbeResult` — Iban.parse's return type is erased to plain `Any?` in Swift (confirmed by
  running this on a macOS runner: no `Result`-shaped wrapper, no `isSuccess`/`getOrNull()`/
  `exceptionOrNull()` survive the interop boundary). On success, the `Any?` holds the `Iban`
  directly (`as? Iban` recovers it). On failure it holds Kotlin's own internal, non-public
  `Result.Failure` box — confirmed via `String(describing:)`, which prints
  `Failure(nl.bijdorpstudio.kiban.IbanParseException...: ...)` — so `as? IbanParseException`
  fails for every failure case: there is no type-safe way to reach the exception from Swift.
- `ProbeExceptions` — casts the erased failure to the top-level `IbanParseException` type;
  fails for the same reason as above.
- `ProbeKindDescribe` / `ProbeKindSwitch` — both cast to `IbanParseException.Malformed` (the
  real generated Swift name, confirmed from the header — nested-type syntax, not a flat
  concatenated name) to reach `.kind`, expected to fail the same way. `ProbeKindSwitch`
  additionally attempts an exhaustive `switch` over `Kind`'s confirmed case names; the real
  header shows `Kind` as an Objective-C class hierarchy, not an `NS_ENUM`, the same
  "Kotlin enum, not a Swift enum" limitation the issue names for this interop path.
- `ProbeUndeclaredThrow` — calls the deprecated, non-`@Throws` `Iban.valueOf` with invalid
  input; confirmed to crash the process (SIGABRT) on a macOS runner, since Kotlin/Native
  terminates on an exception that crosses into Swift without a declared `@Throws`.

```shell
./gradlew :library:assembleKibanDebugXCFramework
mkdir -p samples/swift-console/Frameworks
cp -R library/build/XCFrameworks/debug/Kiban.xcframework samples/swift-console/Frameworks/
cd samples/swift-console
swift run SwiftConsole
swift run ProbeResult
swift run ProbeExceptions
swift run ProbeKindDescribe
swift run ProbeKindSwitch
swift run ProbeUndeclaredThrow  # expected to crash
```

## swift-export-probe

An [XcodeGen](https://github.com/yonaskolb/XcodeGen) project spec (not a committed
`.xcodeproj` — generate it with `xcodegen generate`) for the other half of #9: Swift Export.
Per Kotlin's docs, Swift Export only works via a real, direct-integration Xcode project build
phase — there's no standalone Gradle task for it, unlike everything else in this directory.
This is the minimal such project: one SwiftUI app target whose prebuild script runs
`./gradlew :library:embedSwiftExportForXcode`, and whose `SwiftExportProbeApp.swift` tries the
same `Result<Iban>`/`IbanParseException` probes as swift-console for a direct comparison.

```shell
brew install xcodegen  # if not already installed
cd samples/swift-export-probe
xcodegen generate
xcodebuild build -project SwiftExportProbe.xcodeproj -scheme SwiftExportProbe \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
```
