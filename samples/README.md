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

The package declares several executables:

- `SwiftConsole` — the happy-path walkthrough (parsing, formatting, `isSEPA`, etc).
- `ProbeResult` — exercises `Result<Iban>` from `Iban.parse`/`Iban.compose` (`isSuccess`,
  `getOrNull()`, `exceptionOrNull()`, and whether the erased generic type survives an `as?`
  cast back to `Iban`).
- `ProbeExceptions` — exercises the sealed `IbanParseException` hierarchy via `as?` per
  subtype, since Swift can't switch over it exhaustively.
- `ProbeKindDescribe` / `ProbeKindSwitch` — split so a wrong guess about `Malformed.Kind`'s
  generated Swift case names (`ProbeKindSwitch`) can't hide the safe `String(describing:)`
  output (`ProbeKindDescribe`).
- `ProbeUndeclaredThrow` — calls the deprecated, non-`@Throws` `Iban.valueOf` with invalid
  input; expected to crash the process, since Kotlin/Native terminates on an exception that
  crosses into Swift without a declared `@Throws`.

```shell
./gradlew :library:assembleKibanDebugXCFramework
mkdir -p samples/swift-console/Frameworks
cp -R library/build/XCFrameworks/debug/Kiban.xcframework samples/swift-console/Frameworks/
cd samples/swift-console
swift run swift-console
swift run probe-result
swift run probe-exceptions
swift run probe-kind-describe
swift run probe-kind-switch
swift run probe-undeclared-throw  # expected to crash
```
