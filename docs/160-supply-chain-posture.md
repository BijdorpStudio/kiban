# #160: supply-chain posture for 1.0

#160 asked for three additions, "each independent, land in any order": a CodeQL workflow, an
OpenSSF Scorecard action and badge, and SBOM/build provenance on the published artifacts. Two of
them landed. The third turned out to be already done — CodeQL is enabled on this repository through
default setup, which leaves no file in `.github/workflows` to notice. This is what was chosen for
each and, where an option was rejected, why, so the next person to touch these workflows does not
have to re-derive it.

None of it changes the published API or the library's behaviour. It changes what a consumer can
find out about an artifact before depending on it, which is the part of "ready for 1.0" that
Dependabot and the version-catalog-update plugin — already covering dependency freshness — say
nothing about.

## CodeQL: already enabled, and no workflow to add

The issue asked for a CodeQL workflow. There is nothing to add: **CodeQL default setup is already
enabled on this repository** and has been running all along, analysing both `java-kotlin` and
`actions` on every pull request. Its runs show up under the `dynamic/github-code-scanning/codeql`
path rather than as a file in `.github/workflows`, which is why reading the workflow directory
suggested there was no coverage.

An advanced workflow was written first, on the assumption that there was none, and pushed. It
failed, twice over, and both failures are worth recording because either one is enough to rule the
approach out:

* **The two configurations cannot coexist.** The `analyze` step uploaded its SARIF and got back
  `CodeQL analyses from advanced configurations cannot be processed when the default setup is
  enabled`. Adding a workflow here is not additive — it is a replacement, and switching would mean
  turning default setup off first, which is a repository setting rather than workflow content.

* **`build-mode: manual` produced an empty database.** The step ran
  `./gradlew :library:compileKotlinJvm`, which reported `BUILD SUCCESSFUL` and
  `1 executed, 1 from cache` — the Kotlin compilation was served out of the Gradle build cache, so
  no compiler ever ran under CodeQL's tracer, and `database finalize` failed with `CodeQL could not
  process any code written in Java/Kotlin`. Anything taking this route later has to defeat the
  build cache (and account for the Gradle daemon) for the tracer to see a compilation at all.

The obvious follow-up worry — that default setup's autobuild might extract nothing from a Kotlin
Multiplatform project and pass anyway — does not hold either. On the same commit its `Autobuild`
step ran for 55 seconds and `Perform CodeQL Analysis` for a further 40, against the java-kotlin
database; an autobuild that had seen no code would have failed `database finalize` the way the
advanced run did, in about a second. The coverage is real.

So: nothing to do for this item, and the workflow was removed.

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
