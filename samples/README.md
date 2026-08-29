# Samples

Consumer projects for `kiban`. Two of them (`jvm-cli`, `swift-console`) are about API and interop
ergonomics; the third (`consumption-probe`) is about the published artifact itself.

## jvm-cli

A runnable walkthrough of the API, depending on `:library` directly and following README.md's
"Use" section step by step, printing each result. Not a test suite — running it is the check.

```shell
./gradlew :samples:jvm-cli:run
```

## swift-console

A Swift Package Manager executable exercising the library through Kotlin/Native's
Objective-C interop — the path consumers of the published `Kiban.xcframework` actually use.
Needs a macOS host with Xcode. CI builds and runs it on every push and pull request from
`gradle.yml`'s `swift-console` matrix job (#155), so it cannot quietly stop compiling between
releases; `.github/workflows/ios-interop-verify.yml` runs the same thing on demand, next to
the generated Objective-C header, when the question is what the interop *looks* like rather
than whether it still builds.

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

## consumption-probe

The only thing here that consumes `kiban` the way a real dependant does — by coordinates, out of a
repository, through the Gradle module metadata the publish flow produced. `jvm-cli` depends on
`:library` as a project and never reads a published file, and `verifyPublicationTargets`
(`library/build.gradle.kts`) counts publications rather than resolving one, so between them they
could not tell a working publication from module metadata no consumer can resolve (#154).

It is a separate build, deliberately not included from the root `settings.gradle.kts` and not wired
up with `includeBuild` — either would let dependency substitution put `:library` back in place of
the coordinates. It has no wrapper of its own; the root one drives it with `-p`. Three targets,
`jvm`, `linuxX64` and `js`, one per compilation backend.

```shell
./gradlew -Pkiban.signPublications=false \
  :library:publishKotlinMultiplatformPublicationToMavenLocal \
  :library:publishJvmPublicationToMavenLocal \
  :library:publishLinuxX64PublicationToMavenLocal \
  :library:publishJsPublicationToMavenLocal
./gradlew -p samples/consumption-probe check
```

`linuxX64` makes this a Linux-host check, which is why CI pins the job to a Linux runner. The
publish step names the three consumed targets because the aggregate `publishToMavenLocal` would also
need an Android SDK and every Apple toolchain, and `-Pkiban.signPublications=false` is needed
because no signing key exists outside `publish.yml`.

Being a separate build, it sits outside the root `ktfmtCheck` and `checkKotlinAbi`; its own `check`
runs `ktfmtCheck` for it.
