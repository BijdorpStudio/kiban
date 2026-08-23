# #160: supply-chain posture for 1.0

#160 asked for three additions, "each independent, land in any order": a CodeQL workflow, an
OpenSSF Scorecard action and badge, and SBOM/build provenance on the published artifacts. All
three landed together. This is what was chosen for each and, where an option was rejected, why —
so the next person to touch these workflows does not have to re-derive it.

None of it changes the published API or the library's behaviour. It changes what a consumer can
find out about an artifact before depending on it, which is the part of "ready for 1.0" that
Dependabot and the version-catalog-update plugin — already covering dependency freshness — say
nothing about.

## CodeQL: a workflow, not the default setup, and JVM-only

GitHub's "default setup" is one click in the Security tab and needs no file in the repository. It
autobuilds, which is where it comes apart here: the default Gradle build of a Kotlin Multiplatform
project reaches for the Android SDK and the Apple toolchain, and the `ubuntu-latest` runner CodeQL
schedules has neither. The advanced (workflow) form is what lets the build mode be stated, so
`.github/workflows/codeql.yml` uses `build-mode: manual` and compiles exactly one target.

That target is the JVM one. Every other target compiles the same `commonMain` sources through a
different backend, so `:library:compileKotlinJvm` puts all of the shared code — which is nearly all
of the code — in front of the extractor. What it does not cover is the per-platform `actual`
declarations, a handful of files. Covering those would mean a second target matrix on macOS and
Windows runners for a query pack aimed at the JVM ecosystem; the trade is not worth it, and this
note is here so the gap is known rather than assumed away.

One wart: `CODEQL_EXTRACTOR_KOTLIN_ALLOW_UNSUPPORTED_VERSION`. The Kotlin extractor pins the
compiler versions it can read, and this repository tracks the current Kotlin release, so it is
routinely ahead of that pin. Without the escape hatch the build step fails on the version check and
the workflow goes red on every Kotlin bump until CodeQL catches up — an analysis that runs and may
miss a declaration beats one that never runs. Remove the line if the pin ever leads the Kotlin
version rather than trailing it.

## Scorecard: published results, default branch only

`.github/workflows/scorecard.yml` runs `ossf/scorecard-action` with `publish_results: true`. That
is what backs the README badge and what a consumer querying the Scorecard API or deps.dev reads;
without it the results would reach the Security tab and nowhere else.

Publishing works only from the default branch of a public repository, which fixes the trigger list:
`push` on `main`, a weekly `schedule`, `workflow_dispatch`, and `branch_protection_rule`. No
`pull_request` — the score is a property of `main`, not of a proposed change, and a fork's run could
neither publish nor upload SARIF. `branch_protection_rule` is there because the Branch-Protection
check reads repository settings rather than files: without it, tightening or loosening a ruleset
would not be reflected until the next commit or the next Monday.

Expect the initial score to flag things this repository has deliberately not done — unpinned action
versions being the obvious one, since Dependabot manages the `uses:` majors here rather than SHA
pins. The alerts are a checklist to work through before 1.0, not a target to chase; a low score on a
check whose remediation this project has consciously rejected is a fine outcome, provided the
rejection is written down.

## Provenance, not an SBOM

The issue offered "GitHub artifact attestations […] and/or a CycloneDX SBOM". The attestations
landed; the SBOM did not.

The reason is that the two answer different questions, and only one of them is open here. An SBOM
enumerates what went *into* an artifact — and kiban's answer is already published, in the POM and
the Gradle module metadata that every consumer resolves: it depends on nothing but the Kotlin
standard library, performs no I/O and vendors no code. A CycloneDX document would restate that, and
the plugin that produces one is built around JVM configurations rather than a multiplatform target
set, so it would restate it for one target and stay quiet about the other sixteen.

Provenance answers the question the existing tooling genuinely cannot. The GPG signature on a Maven
Central artifact says the maintainer's key signed *something*; it does not say which commit, which
workflow or which runner produced the bytes. `actions/attest-build-provenance` binds each published
file's digest to this repository, this workflow and this commit, verifiable with
`gh attestation verify <file> --repo BijdorpStudio/kiban` against a jar pulled from Maven Central.
That is the check a consumer in a regulated environment is actually asked to perform.

### Why the publish job stages a local Maven repository

Attestation takes files as subjects, and after `publishToMavenCentral` there is no one directory
holding them: a Kotlin Multiplatform publication is 17 modules whose artifacts sit wherever their
compilation left them — `build/libs` for the JVM and metadata jars, a per-target
`build/classes/kotlin/<target>/main/klib` for the Native ones, `build/outputs` for Android. A glob
spanning all of that would be a guess that goes quietly wrong the next time the Kotlin Gradle
plugin moves an output.

So the publish job runs `:library:publishToMavenLocal` a second time with
`-Dmaven.repo.local=$GITHUB_WORKSPACE/build/attestation-staging`, which writes every published file
into one tree, in the layout Maven Central will serve it in, inside the workspace where a relative
glob can reach it. It is a second *write*, not a second build: every compile, jar and `Sign` task is
up to date by then, so the cost is Gradle's configuration time plus the install.

The staging step counts what it wrote and fails with a diagnosable message if the count is zero,
because the alternative failure — `actions/attest-build-provenance` reporting "no subjects found"
at the very end of a release — says nothing about which of the two possible causes (a changed
publication layout, or Gradle ignoring `-Dmaven.repo.local`) it hit.

Subjects are the binaries only: `.jar`, `.klib` and `.aar`. The `.pom` and `.module` files are
metadata nothing executes, and leaving them out keeps the list to the ~51 files that end up on a
consumer's classpath.
