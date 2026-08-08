# Samples

Consumer projects for `kiban`, focused on API/interop ergonomics rather than Maven packaging
(the publish flow, `apiCheck`, and `verifyPublicationTargets` already cover the packaging side).

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

- `SwiftConsole` — the happy-path walkthrough (parsing, formatting, `isSEPA`, etc). Needs
  `import Foundation`: Kotlin extension functions on `String` (`toIbanOrNull()`,
  `isValidIban()`) are exported as an NSString category, which Swift only resolves on its
  native `String` via Foundation's bridging.
- `ProbeResult` — Iban.parse's return type is erased to plain `Any?` in Swift (confirmed by
  running this on a macOS runner: no `Result`-shaped wrapper, no `isSuccess`/`getOrNull()`/
  `exceptionOrNull()` survive the interop boundary). Probes what's still recoverable via
  `as? Iban` / `as? IbanParseException` on that bare `Any?`.
- `ProbeExceptions` — casts the erased failure to the top-level `IbanParseException` type.
- `ProbeKindDescribe` / `ProbeKindSwitch` — split so a wrong guess about the nested
  `Malformed` class name or `Kind`'s generated Swift case names (`ProbeKindSwitch`) can't
  hide the safer `String(describing:)` output (`ProbeKindDescribe`).
- `ProbeUndeclaredThrow` — calls the deprecated, non-`@Throws` `Iban.valueOf` with invalid
  input; expected to crash the process, since Kotlin/Native terminates on an exception that
  crosses into Swift without a declared `@Throws`.

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
