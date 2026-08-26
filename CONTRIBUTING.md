# Contributing to kiban

Issues, discussion and pull requests are all welcome. This document is the practical part: what to
run before you push, what the repository expects a change to carry with it, and which checks cannot
run on every machine. The policy that decides what a change *means* for consumers — major, minor or
patch, and what the binary compatibility guarantee covers — is [VERSIONING.md](VERSIONING.md); the
mechanics of cutting a release are in [RELEASING.md](RELEASING.md).

Reporting a security vulnerability is a separate path: do not open an issue, see
[SECURITY.md](SECURITY.md).

## What you need

A JDK 17 or newer is the only prerequisite. Gradle comes from the wrapper — always invoke
`./gradlew`, never a locally installed `gradle`, so everyone runs the version the repository pins.
The Kotlin, Android and Kotlin/Native toolchains are downloaded by the build itself.

Two parts of the build need more than that, and neither is expected of a contributor:

* **Apple targets** (`ios*`, `macos*`, `tvos*`, `watchos*`, `assembleKibanDebugXCFramework` and
  `samples/swift-console`) need macOS with Xcode. They cannot be built on Linux or Windows at all.
* **Android-specific tasks** (`:library:testAndroidHostTest`, `assembleAndroidMain`, lint) need an
  Android SDK, i.e. `ANDROID_HOME` pointing at an installation. Without one they fail with
  "SDK location not found", which is a missing SDK rather than anything wrong with the change.

CI runs the full target matrix — Linux, macOS and Windows runners, the Swift sample included — on
every pull request (`.github/workflows/gradle.yml`). That is what verifies the targets your machine
cannot build, so a green local run plus a green CI run is the complete picture.

## Before you push

```
./gradlew jvmTest apiCheck ktfmtCheck
```

These three run everywhere, including on Linux and Windows without an Android SDK, and they catch
most of what CI would reject:

* **`jvmTest`** runs the common test suite on the JVM. The same sources run on every other target in
  CI, so a failure here is a failure everywhere.
* **`apiCheck`** compares the public API against the dumps committed under `library/api/`
  (`jvm/library.api` and `library.klib.api`). The klib half infers the Apple targets from the
  buildable ones, so this passes on Linux — you do not need a Mac to check the API.
* **`ktfmtCheck`** enforces formatting. `./gradlew ktfmtFormat` applies it; run that rather than
  hand-fixing the report.

## Changing the public API

`apiCheck` fails on any change to the public API, including additions. That is deliberate: the dumps
are the reviewable record of what consumers can see, and [VERSIONING.md](VERSIONING.md) defines the
public API as exactly what they contain.

When a change to the API is intended, regenerate and commit the dumps:

```
./gradlew apiDump
```

Both dump files are expected in the same commit as the code that changes them. Note that a new
optional parameter on an already-published function is source-compatible but *not* binary
compatible — see the `@IntroducedAt` rule in [VERSIONING.md](VERSIONING.md) before adding one.

## Tests

Tests live in `library/src/commonTest` and run on every target. The suite uses
[TestBalloon](https://github.com/infix-de/testBalloon) with [assertk](https://github.com/willowtreeapps/assertk)
assertions — a test file declares `val SomethingTest by testSuite { … }` holding `test("…") { … }`
blocks, rather than annotated methods; follow the shape of the neighbouring files. A table of inputs
loops around the `test { … }` declaration rather than inside a single test body, so a failure names
the case that failed instead of stopping at the first one.

New behaviour needs a test, and a bug fix needs one that fails without it.

## IBAN registry data

`CountryCodesData.kt` and `CountryTestData.kt` are **generated** — do not hand-edit them. They come
from the SWIFT IBAN Registry TXT via `scripts/generate_country_data.main.kts`; the README's
["Updating the IBAN registry data"](README.md#updating-the-iban-registry-data) section has the
sequence, and the raw registry file is never committed because it is not redistributable. A weekly
workflow (`.github/workflows/registry-sync.yml`) opens the update pull request when the registry
changes, so this is rarely something to do by hand.

Both scripts under `scripts/` carry their own offline test: `--self-check` on either one asserts
over its parsing helpers and exits. Changing how the generator reads the registry means extending
`scripts/testdata/synthetic-registry.txt` — there is no real registry file to test against.

## Pull requests

* **One topic per pull request.** Keep unrelated cleanups out of a change that has to be reviewed on
  its merits.
* **Reference the issue** it addresses, if there is one.
* **Say what you ran.** List the Gradle commands you actually executed and name what you could not
  verify locally (Apple targets and Android tasks, typically). Do not describe an unrun check as
  passing — CI will run it, and an honest gap is more useful than a claim.
* **Add a CHANGELOG entry** under the top `## X.Y.Z (unreleased)` section of
  [CHANGELOG.md](CHANGELOG.md) for anything a consumer would notice, in the appropriate group
  (breaking changes first). The existing entries show the expected level of detail: what changed and
  why it was the right call, not just what moved.
* **Let CI go green** before asking for a merge. The matrix is the real verification.
* Every source file carries the Apache 2.0 licence header — copy it from a neighbouring file when
  adding one.

## Automated agents

Coding agents work on this repository from cloud sandboxes. [CLAUDE.md](CLAUDE.md) records which
Gradle tasks do and do not run in such a container and what "verified" is allowed to mean there. It
is a note to those agents, not contributor documentation — this file is the human one.
