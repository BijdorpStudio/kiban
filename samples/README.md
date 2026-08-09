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

One thing to know before reading it, a finding of the #9 interop review
([`docs/9-swift-interop-review.md`](../docs/9-swift-interop-review.md)): top-level Kotlin
extension functions (`toIbanOrNull`, `isValidIban`, `toIban`) surface as static methods on an
`IbanKt` facade class, not as Swift extensions on `String`.

`Iban.parse`/`Iban.compose` used to erase to an opaque `Result<Iban>` under Objective-C interop
(#9's headline finding, since fixed in 0.5.0 — see `docs/9-swift-interop-review.md` for what the
old shape looked like from Swift). The throwing entry points are now annotated
`@Throws(IbanParseException::class)`, so the sample also demonstrates catching a failure as a
Swift error. The probe executables from the original #9 investigation live on the
`swift-export-playground` branch, alongside the Swift Export experiment.

```shell
./gradlew :library:assembleKibanDebugXCFramework
mkdir -p samples/swift-console/Frameworks
cp -R library/build/XCFrameworks/debug/Kiban.xcframework samples/swift-console/Frameworks/
cd samples/swift-console
swift run SwiftConsole
```
