# Samples

Consumer projects for `kiban`, focused on API/interop ergonomics rather than Maven packaging
(the publish flow, `apiCheck`, and `verifyPublicationTargets` already cover the packaging side).

## jvm-cli

A JVM console app depending on `:library` directly, and the home for the assertions that keep
README.md's "Use" section executable — see `ReadmeExamplesTest.kt`'s doc comment for why this
exists (#60's undetected `Modulo97.calculateCheckDigits("XX", "X")` documentation bug).

```shell
./gradlew :samples:jvm-cli:test
./gradlew :samples:jvm-cli:run --args="NL91ABNA0417164300"
```

## swift-console

A Swift Package Manager executable exercising the library through Kotlin/Native's
Objective-C interop — the concrete testbed #9 needs to evaluate that (and, later, Swift Export)
for real. Needs a macOS host with Xcode; see `.github/workflows/ios-interop-verify.yml` for
on-demand CI access to one.

```shell
./gradlew :library:assembleKibanDebugXCFramework
mkdir -p samples/swift-console/Frameworks
cp -R library/build/XCFrameworks/debug/Kiban.xcframework samples/swift-console/Frameworks/
cd samples/swift-console
swift run
```
