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
