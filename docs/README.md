# Design notes

Each page here records one decision: a spike that had to choose between options, or a piece of
infrastructure whose reasoning is too long to live in a source comment. The page states what was
chosen, what was rejected, and the evidence behind it, so the next person to touch that area does
not have to re-derive it.

These are a record of how the project got here, not a description of how it behaves today. For
that, read [README.md](../README.md) (what the library does),
[VERSIONING.md](../VERSIONING.md) (the stability and target policy),
[CONTRIBUTING.md](../CONTRIBUTING.md) (how to work on it) and
[RELEASING.md](../RELEASING.md) (how a release is cut). A page is not rewritten when a later change
supersedes its decision; the [CHANGELOG.md](../CHANGELOG.md) entry for that change is where the
newer answer lives.

File names are title-first, so the directory listing reads as a table of contents. Each page links
the issue it answers in its first lines, and the issue is listed below too.

## API and library design

* [swift-interop-review.md](swift-interop-review.md) — how kiban's API surfaces through
  Kotlin/Native's Objective-C interop and through Swift Export, checked against real compiled and
  executed Swift. Recommends adding a small Swift-friendly companion surface before 1.0, because
  the Objective-C path makes a `Result<Iban>` failure unrecoverable from Swift and Swift Export is
  both Alpha and unable to serve published-binary consumers. (#9)
* [iban-value-class-evaluation.md](iban-value-class-evaluation.md) — whether `Iban`, whose only
  stored state is one validated `String`, should become a `@JvmInline value class` before the API
  freeze. Decision: no, keep it a regular `class`. (#98)
* [instant-api-stability.md](instant-api-stability.md) — whether `kotlin.time.Instant`, the type
  of `CountryCodes.lastUpdateDate`, is stable enough to freeze into the 1.0 API. Conclusion: yes;
  it is non-experimental from Kotlin 2.3, which is what pins the Kotlin floor from below. (#144)
* [test-fixtures-investigation.md](test-fixtures-investigation.md) — where consumer-facing test
  helpers such as a per-country `Iban.random()` should live, given they should not ship in the main
  artifact. Recommends a separate published `kiban-test` multiplatform module over Gradle's
  `java-test-fixtures`, whose fixtures variants never reach the published module metadata, so an
  external consumer cannot resolve them at all. (#67)

## Build and test tooling

* [builtin-abi-validation.md](builtin-abi-validation.md) — whether to move off the standalone
  `binary-compatibility-validator` plugin, now in maintenance mode, onto the ABI validation built
  into the Kotlin Gradle plugin. Conclusion: migrated; the dumps carry over in place and the klib
  coverage is byte-identical. (#182)
* [testballoon-evaluation.md](testballoon-evaluation.md) — whether to migrate the test suite from
  `kotlin.test` to TestBalloon, and what the migration would cost. Recommends migrating once
  TestBalloon 1.1.0 is released, keeping assertk and dropping `kotlin-test` entirely; the migration
  landed later under #115. (#82)
* [develocity-evaluation.md](develocity-evaluation.md) — whether to adopt Develocity for build
  scans and build caching. Recommends publishing free public build scans from CI only, wired
  through the `gradle/actions/setup-gradle` inputs rather than the `com.gradle.develocity`
  settings plugin, and not pursuing the OSS program. (#116)

## CI and release infrastructure

* [intel-target-policy.md](intel-target-policy.md) — whether the two Intel Apple targets should be
  treated alike, by reinstating `macosX64` or by dropping `iosX64` as well. Decision: neither, the
  published target set stays exactly as it is. (#207)
* [versioned-api-docs.md](versioned-api-docs.md) — how the Dokka versioning plugin is wired into
  the release so that a consumer on an older version can still reach its API reference, and why
  the archive of older versions is stored the way it is. (#162)
* [supply-chain-posture.md](supply-chain-posture.md) — what a consumer can find out about a
  published artifact before depending on it: the OpenSSF Scorecard workflow and badge, SBOM and
  build provenance on the published artifacts, and why the CodeQL workflow the issue asked for
  needed no file (default setup already runs it). (#160)
