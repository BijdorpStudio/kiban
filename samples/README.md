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
Objective-C interop — the path consumers of the published `Kiban.xcframework` actually use.
Needs a macOS host with Xcode; see `.github/workflows/ios-interop-verify.yml` for on-demand
CI access to one.

Two things to know before reading it, both findings of the #9 interop review
([`docs/9-swift-interop-review.md`](../docs/9-swift-interop-review.md)):

- Top-level Kotlin extension functions (`toIbanOrNull`, `isValidIban`) surface as static
  methods on an `IbanKt` facade class, not as Swift extensions on `String`.
- `Result<Iban>`-returning APIs (`Iban.parse`, `Iban.compose`) erase to an opaque `Any?` with
  no typed access to the failure, so the sample sticks to `toIbanOrNull`/`isValidIban`. The
  probe executables that established this live on the `swift-export-playground` branch,
  alongside the Swift Export experiment.

```shell
./gradlew :library:assembleKibanDebugXCFramework
mkdir -p samples/swift-console/Frameworks
cp -R library/build/XCFrameworks/debug/Kiban.xcframework samples/swift-console/Frameworks/
cd samples/swift-console
swift run SwiftConsole
```
