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

The alerts a run produces are a checklist to work through before 1.0, not a target to chase; a low
score on a check whose remediation this project has consciously rejected is a fine outcome, provided
the rejection is written down. Two checks that would have scored against this repository are now
done rather than rejected — see below, including why no run has actually said so yet.

## Pinned-Dependencies and token persistence: both done (#222)

Two Scorecard checks had an obvious answer here, and #222 closed both.

**Every `uses:` reference is pinned to a full commit SHA**, with the version it resolved to as a
trailing comment (`uses: actions/checkout@3d3c42e5… # v7.0.1`). Pinning by major tag — which is what
these were — means a moved tag silently changes what runs, and one of the jobs it would change is
`publish`, which holds the signing key and the Central token. Dependabot's `github-actions`
ecosystem updates SHA pins and rewrites the trailing comment along with them, so the pins do not
freeze the actions at today's versions; `.github/dependabot.yml` already lists both `/` (the
workflows) and `/.github/actions/cache-konan` (the composite action, a separate manifest), so
nothing escapes that coverage.

Pinning also fixed a live failure, and this is the one argument against major-tag pinning that
reading the file does not give you: `scorecard.yml` referenced `ossf/scorecard-action@v2`, and that
repository publishes no floating `v2` tag — only `v2.x.y`. All 30 runs since the workflow landed in
August failed, every one of them in "Set up job" before a step executed, which is where an
unresolvable `uses:` reference fails. So the workflow has never produced a score; the badge and the
Security tab have been empty rather than stale. The pin is `v2.5.0`'s commit.

**`persist-credentials: false` on every `actions/checkout` but one.** Without it `actions/checkout`
leaves the `GITHUB_TOKEN` in `.git/config`, where every later step in the job can read it — in this
repository that means the Gradle build, and through it any dependency's build logic. The exception
is `registry-sync.yml`: that job exists to push a branch and open a pull request, so its checkout
keeps the credential it was handed. `archive-docs` pushes too but needs nothing persisted; it builds
a fresh repository and pushes with the token in the remote URL.

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

## The dependency graph: what the SBOM question was really asking

The SBOM was declined above because the POM and the Gradle module metadata already publish what
kiban depends on. That holds for *consumers* of a release. It does not cover the other direction —
what GitHub knows about this repository's own build — and #224 closed that gap with
`.github/workflows/dependency-submission.yml`.

The distinction matters because the two look interchangeable and are not. `.github/dependabot.yml`
gives version updates: Dependabot reads `gradle/libs.versions.toml` and opens a pull request when a
declared version moves. Dependabot *alerts* work from the dependency graph instead, and until
something submits one, the graph for a Gradle repository holds only what the catalog scan found —
the declared modules, none of their transitive dependencies. A CVE in something kiban pulls in
rather than declares would therefore raise nothing at all, which is the one gap the rest of this
posture leaves open: Scorecard scores the repository's practices and provenance binds an artifact
to the run that built it; neither looks at what is on the classpath.

`gradle/actions/dependency-submission` closes it by resolving every configuration in every project
and posting the result to the Dependency Submission API, so the graph is Gradle's own resolution
result rather than an approximation of it — all sixteen targets, not just the JVM.

Two things about the workflow are deliberate and worth not re-deriving:

* **`main` only, no `pull_request` trigger.** Submission needs `contents: write`, which the token
  of a pull request from a fork does not get, and the graph is a property of the default branch —
  the same two reasons `scorecard.yml` has no `pull_request` trigger either. A proposed dependency
  change is reviewed in the version catalog diff.
* **`--no-configuration-cache`.** `org.gradle.configuration-cache` is on in `gradle.properties`,
  and the action's injected dependency-graph plugin does not document configuration-cache support;
  the action's own documentation uses this flag as its example for `additional-arguments`. The job
  is one resolution pass on `main`, so configuring from scratch costs nothing worth the risk — and
  the failure it avoids is the quiet one, a graph that stops being updated rather than a red check.
