# Samples

Consumer projects that resolve `kiban` by Maven coordinates against a locally-published
artifact, not by depending on the `:library` Gradle project directly. This is what #68 is for:
an in-repo module bypasses the published artifact shape entirely (broken POMs, missing target
variants), so these samples resolve the real thing instead.

Both samples are deliberately outside the root build (`settings.gradle.kts` does not
`include()` them) so Gradle project substitution can't quietly swap in `:library` for the
dependency and defeat the point.

## jvm-cli

A JVM console app, and the home for the assertions that keep README.md's "Use" section
executable — see `ReadmeExamplesTest.kt`'s doc comment for why this exists (#60's undetected
`Modulo97.calculateCheckDigits("XX", "X")` documentation bug).

```shell
./gradlew publishToMavenLocal   # from the repo root; publishes the version jvm-cli depends on
./gradlew -p samples :jvm-cli:test
./gradlew -p samples :jvm-cli:run --args="NL91ABNA0417164300"
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
