# CI workflow notes

Issue: [#209](https://github.com/BijdorpStudio/kiban/issues/209)

Why the workflows under `.github/` are wired the way they are. The Gradle configuration they drive
has its own page, [209-build-script-notes.md](209-build-script-notes.md). `RELEASING.md` is the
procedure; this is the reasoning behind the mechanics.

## `gradle.yml` — the check matrix

The single required check is the `ci` job, not the matrix. `needs` on a matrix job waits for every
entry, so adding, renaming or removing a matrix target never touches the list of required checks.
`if: always()` is what makes it usable as that check: without it the gate is skipped whenever a
matrix entry fails, and a required check that never reports leaves the pull request waiting forever
instead of going red. `needs.build.result` is the combined result of all matrix entries — `success`
only when every one of them succeeded.

### Concurrency

The full target matrix — a job per target, several of them on macOS runners — against a commit that
has already been superseded tells nobody anything, so a new push to a branch or pull request cancels
the run still going for the old head. Two cases keep every run instead:

* `main`, where the per-commit result is the record of what the branch actually passed.
* Release runs, which reach this workflow through `workflow_call` from `publish.yml`. There
  `github.ref` is the release tag, and `publish.yml` fires on both `prereleased` and `released`, so
  promoting a prerelease starts a second run against the same tag — cancelling the first would fail
  the `needs: test` gate and take its publish job down with it.

### Matrix shape

Each entry carries an `id` alongside its `target`. The job is named off `matrix.id` alone so the
check name stays stable when a matrix key is added or reordered — the default name interpolates
*every* matrix value — and the `id` is the slug used in artifact names and step conditions, because
matrix targets contain spaces and colons, which are not valid in artifact names and would collide
once sanitised away.

A few entries are not simply "run the target's test task":

* **`linuxArm64Test`** is not a task KGP registers. `:kiban` registers it itself and runs the binary
  under the emulator the toolchain ships — see
  [209-build-script-notes.md](209-build-script-notes.md#linuxarm64test-runs-under-the-qemu-emulator).
  It reports like any other test task from here.
* **`checkKotlinAbi`** builds a klib for every target the host supports, so it doubles as the
  compile check for the Apple targets.
* **`dokka`** is generation only, no deploy: it catches broken KDoc or `sourceLink` before a release
  tag is cut. The actual publish to GitHub Pages stays in `publish.yml`.
* **`swift-console`** assembles the XCFramework the Swift sample consumes, and a later step compiles
  and runs the sample against it. Cheap enough to run per pull request: the XCFramework holds a
  single target (`macosArm64`), so this is one debug link plus a `swift build`, next to the four
  full Apple test jobs the matrix already runs. The sample is a Swift Package Manager build outside
  Gradle, and its `Package.swift` resolves the binary target out of `Frameworks/`, which is
  git-ignored and populated per build — the same three lines as `samples/README.md` and
  `ios-interop-verify.yml`.
* **`consumption-probe`** publishes to `mavenLocal` what the step after it consumes. Three targets
  rather than the aggregate `publishToMavenLocal`, which also publishes the Android and Apple
  variants and so would need an Android SDK and a macOS runner to check something that is about
  metadata. The probe is a build of its own so it resolves kiban by coordinates out of the local
  repository rather than being substituted back to `:kiban` — see `samples/consumption-probe`.

### Build scans

Scans go to the public `scans.gradle.com`.
[116-develocity-evaluation.md](116-develocity-evaluation.md) chose the `setup-gradle` action inputs
over the `com.gradle.develocity` settings plugin deliberately: the action injects the plugin via init
script, so `settings.gradle.kts`, the version catalog and `gradle.properties` stay untouched, and
local plus cloud-sandbox builds never load it, never attempt a publish and never leak a developer's
machine details to a public dashboard.

Only this workflow is scanned: it owns the matrix and therefore the payoff. `publish.yml`,
`registry-sync.yml` and `ios-interop-verify.yml` also use `setup-gradle` but are low-frequency, and
stay unscanned.

Scans are **public**. Everything uploaded originates on an ephemeral public runner building a public
repository — build structure, task, dependency and test data and console output; no source archives
and no developer environment. Terms of use were accepted for the project by the maintainer on #127.

### Reporting

`shell: bash` on the Gradle step enables pipefail, so `tee` does not mask a Gradle failure. The log
it writes is the only diagnosable output for targets that write no report of their own, such as
`:samples:jvm-cli:run`.

The JUnit report step renders the XML Gradle already writes into the job summary and annotates
failing tests on the run. `always()` is the point of it: per-test results have to reach the summary
on a green run too, not just a red one. The other inputs follow from that:

* `annotate_only` keeps it working under the workflow's `contents: read` token and on pull requests
  from forks — creating a check run instead would need `checks: write`, which a fork's read-only
  token does not have.
* `include_passed: false` gives counts on green and the grouped failure listing on red. Listing
  every passing test was tried and reverted: eleven test jobs each rendering the full per-country
  table buried the two numbers anyone opens a summary for. The full listing is still a download away
  in the `test-results-*` artifact.
* `fail_on_failure: false`, because the Gradle step has already failed the job by this point.
  Failing again here would only replace a diagnosable error with this action's own.
* `continue-on-error`, because reporting is a convenience wrapped around the real signal. If the
  action itself breaks it must not fail the job — and, because `failure()` latches for the rest of
  the job once any step fails, it must not make the two diff steps fire on an otherwise green run
  either.
* The step is skipped where no test XML exists at all, rather than reporting an empty suite.

The `ci` job repeats the roll-up across every matrix job's XML, on the one job a reviewer is certain
to open. Suite names carry their target (`IbanTest[jvm]`, `IbanTest[iosSimulatorArm64]`, …), so
grouping by suite attributes each result without the matrix jobs having to be listed out there.
`skip_annotations` is set because the matrix jobs have already annotated their own failures;
repeating them would double every one of them on the pull request. Both of its steps are
`continue-on-error` on purpose: this job is the single required check, so a missing artifact or a bad
parse has to cost the summary and nothing else, and must never be able to turn a green matrix red.

Test XML is uploaded on green as well as red — it is a few hundred KB and it is what the `ci` job
aggregates. On a red run it overlaps with the failure artifact, which is still the richer of the pair
(HTML reports, API dumps) and stays as it is. Not every target writes every kind of report, so an
empty match there is not itself a failure worth flagging.

### The two failure-diff steps

`ktfmtCheck` fails with a bare file list; re-formatting turns that into a patch that can be
downloaded and applied. `checkKotlinAbi` reports a binary-compatibility break as a Gradle failure
whose useful part — what the dump would have to become — is buried in the log tail; regenerating the
dump surfaces exactly that, as a reviewable diff. Both are also rendered into the job summary,
because the usual case is a handful of lines, not worth a round trip through an artifact download to
read. Both are truncated to 400 lines, because the step summary is capped at 1 MiB and a run that
blows past that is one where the artifact is the right tool anyway.

## `publish.yml`

`verify-version` fails fast, before the full check matrix and the publish job spin up, if the release
tag does not match the version the build declares. The workflow fires on any
`released`/`prereleased` event and ships whatever the checked-out build says, so tagging `v1.0.0`
against a commit still declaring an older version would silently republish that version — and only
fail late at Maven Central, if at all. The version is read with `sed` rather than by booting Gradle
because the whole point of the job is to fail fast.

`permissions: contents: read` at the workflow level is least privilege for every job. Without it the
`GITHUB_TOKEN` falls back to the repository default, which is read-write for repositories created
before February 2023 — and the `publish` job runs with the Maven Central and GPG signing secrets in
scope. Two jobs widen it: `publish` for attestation, `archive-docs` for the branch it pushes.

The `publish` job is the only one holding those credentials. Binding it to a GitHub environment is
what makes that scoping enforceable: the secrets can live on the `release` environment instead of
the repository, so no other workflow or job can read them, and the environment's required reviewers
turn "a release was created" into "a human approved this publish". Creating a release stays the
trigger; it stops being sufficient on its own. The environment and its protection rules are
repository settings, not workflow content — see [RELEASING.md](../RELEASING.md) for what has to be
configured for this to gate anything.

`kotlin.native.ignoreDisabledTargets` stays `true` in `gradle.properties` for local dev and PR CI,
where no single host builds every target. It is forced `false` here so a target this runner cannot
build fails the build instead of silently shipping a partial artifact set — which is also why this
job needs the whole Kotlin/Native distribution and so the konan cache.

### Build provenance

See [160-supply-chain-posture.md](160-supply-chain-posture.md) for the posture this is part of.
`attestations: write` records the attestation against this repository; `id-token: write` is how the
run proves to Sigstore that it is this workflow on this commit, rather than by holding a signing key.
Neither widens what the job can do to the repository's contents.

What it buys a consumer: `gh attestation verify <file> --repo BijdorpStudio/kiban` on a jar or klib
pulled from Maven Central answers "was this file built by this repository's release workflow, from
this commit" — a question the GPG signature, which says only that the maintainer's key signed
something, cannot answer.

Attestation needs the files as subjects, and the publish step uploads them without leaving a single
directory to point at: a Kotlin Multiplatform publication is 17 modules whose artifacts sit wherever
their compilation put them (`build/libs` for the JVM and metadata jars,
`build/classes/kotlin/<target>/main/klib` for the Native ones, and so on). Writing the same
publication into a local Maven repository lays every published file out under one path, in the layout
Maven Central will serve it in. That staging step is a second *write* of artifacts the publish
already built, not a second build: every compile, jar and `Sign` task is up to date by then, so it
costs the install itself plus Gradle's configuration time. `-Dmaven.repo.local` keeps it inside the
workspace rather than in `~/.m2`, which is what lets the attest step address it with a relative
glob. The signing secrets are passed because `signAllPublications()` applies to every repository, so
without them the publication would fail on a missing signatory. An empty tree would make the attest
step fail with "no subjects found", which says nothing about why, so the staging step counts what it
produced and fails there instead.

Binaries only. The `.pom` and `.module` files are metadata that no consumer executes, and leaving
them out keeps the subject list to the files that actually end up on a classpath.

### Docs and the docs archive

The `docs` job restores the versions already published from the `docs-archive` branch.
`continue-on-error` covers the one case where that branch does not exist: the first release after
versioned docs landed. The generate step then falls through to the single-version site, and the
archive starts existing from that release onwards. The `kiban.previousDocVersions` property is only
passed when there is actually an archive to merge in: pointing the versioning plugin at a directory
holding no versions buys nothing, and not passing it at all is the documented way to ask for the
plain single-version site. Dokka resolves `nativeMain`/`appleMain` through the Kotlin analysis
frontend, so this job needs the Kotlin/Native distribution too.

`archive-docs` adds the version just published to that archive. It is a job of its own for one
reason: `contents: write` — what it takes to push a branch — must not sit on a job that runs the
Gradle build, where any dependency's build logic would inherit it. This job runs no project code; it
rearranges files and pushes them. It archives the Pages artifact the run just deployed rather than
re-generating anything, so what ends up behind the dropdown is byte for byte what was served as that
version.

Inside it: `verify-version` has already established that the tag and the project version agree, so
the tag is a safe source for the version. Everything served under `older/` is a version that was
already archived, carried over unchanged so the branch stays the complete set. What sits at the root
is the version just published, and its own copy of `older/` is dropped before archiving — keeping it
would nest every past version inside every future one, and the archive would double in size with
every release. The branch is a fresh repository with a single commit, force-pushed: it is a snapshot,
not a history, so it never accumulates the previous releases' copies as dangling git objects, and
nothing reads its history — the `docs` job only ever checks out its tip.

Both the `publish` and `docs` jobs run on a release-tag ref, never on `main` or `release`, so the
Gradle cache is read-only in both. They reuse entries the `main` CI build wrote; a one-shot release
build has nothing worth writing back.

## `registry-sync.yml` — the scheduled SWIFT registry sync

It detects new SWIFT IBAN Registry revisions and opens a data-update pull request without anyone
having to download the registry by hand. The download endpoint blocks plain HTTP clients below the
HTTP layer, so acquisition runs through a real Chromium context
(`scripts/fetch_registry.main.kts`); generation then runs the same
`scripts/generate_country_data.main.kts` a maintainer would run locally.

The raw registry TXT is never committed — it is not redistributable. It only ever exists in the job
workspace under `scripts/input/` (gitignored), and the pull request carries just the derived
artifacts: `CountryCodesData.kt` and the country test data table.

### Why the job does not ask for a revision up front

The registry's release number is not published anywhere a machine can read: the download sends no
`Content-Disposition` filename and the page states no release. So the job regenerates the data using
the revision and datestamp already committed — metadata the generator only stamps into its output —
and diffs. Both are reused deliberately: it keeps a run where the registry did not change
byte-identical to `HEAD`, so "nothing changed" cannot turn into a weekly no-op pull request.
Unchanged is the outcome of all but a couple of runs a year, and means the job is done without a
revision ever being needed. Changed means a maintainer re-runs it with the `rev` input, which is
worth the interruption exactly then.

`ktfmt` is not optional in those regeneration steps: KotlinPoet's raw output differs from the
committed files in whitespace alone (license header indent, comment wrapping), so an unformatted
regeneration reports ~1700 changed lines even when every byte of registry data is identical.

The generator's self-check runs *before* the download, not after: if the generator can no longer
read the registry format, the run has nothing to contribute and should say so against a fixture
rather than against whatever the registry happens to be today.

### Headed Chromium under Xvfb

There is no headless attempt first. Headless Chromium is dropped by Swift's edge from a consumer ISP
and from GitHub's runner range alike, which puts the tell in the client fingerprint —
`navigator.webdriver`, the `HeadlessChrome` UA token, no GPU — rather than in the origin IP. So a
headless attempt is a guaranteed ~60s of every run buying a signal nothing acts on: if it ever
started working it would save ten seconds of Xvfb startup. Do not re-add it as an optimisation. The
script still defaults to headless, which is right for anyone whose network is not blocked and for
hosts without a display.

### Best-effort by design

A failure only costs the convenience — manually downloading the TXT into `scripts/input/` and running
the generator stays the guaranteed fallback. A failed run therefore means something actually broke.
The schedule is weekly: the registry only changes once or twice a year, but a run that finds nothing
costs about two minutes and stops silently, and checking often shortens the window in which a
bot-detection change goes unnoticed.

### `scripts/fetch_registry.main.kts`

Swift blocks unrecognised clients below the HTTP layer, so no shell script or HTTP library ever sees
a status code — the connection just hangs and is eventually reset. That covers the registry page
itself, not only the download endpoint, which is why failures surface as navigation timeouts rather
than as a bot-check page. A real Chromium context that has actually loaded the page can fetch it: a
programmatic `fetch()` from page context returns the TXT with HTTP 200. Headless Chromium is itself
blocked from some networks even where headed works, so `--headed` is a genuine fallback and not just
a debugging aid.

The revision cannot currently be detected: the download endpoint sends no `Content-Disposition`
header at all, and the registry page names no release number anywhere. The detection in the script
is therefore best-effort against a page that may start stating one again, and an unknown revision is
the normal outcome rather than a failure — it only has to be resolved once the registry TXT has
actually changed, which is why `registry-sync.yml` regenerates and diffs first and demands a revision
second.

Bot detection is an arms race this project does not control, so the automated path is best-effort
convenience throughout: downloading the TXT manually in a browser into `scripts/input/` and running
the generator by hand stays the guaranteed fallback, and nothing is lost but convenience if this
stops working.

### `scripts/generate_country_data.main.kts`

The bank and branch identifier positions are validated against the registry's own independent "Bank
identifier example" and "Branch identifier example" fields, so the position encoding is cross-checked
against data it was not derived from rather than against itself.

`--self-check` runs against invented countries in the registry's own format under
`scripts/testdata/`, because the real TXT cannot be committed. It is what turns a format-handling
regression into a failing check rather than a bad weekly sync, which is why `registry-sync.yml` runs
it before the download.

### What the job needs from the repository

Actions must be allowed to create pull requests (Settings → Actions → General). Beyond that, the
pull request is opened with the `REGISTRY_SYNC_TOKEN` secret whenever it is set: a fine-grained PAT
or GitHub App installation token scoped to this repository with `Contents: read and write` and
`Pull requests: read and write`. That secret is what gets a data-update pull request the same
target-matrix verification as every other pull request — pull requests opened with the default
`GITHUB_TOKEN` do not trigger further workflow runs, so `gradle.yml` never sees one. With the secret
unset the job still works: it falls back to `GITHUB_TOKEN`, and the pull request body then says to
close and reopen it for a full run before merging. Either way the job runs `./gradlew jvmTest`
itself, so the data is validated before the pull request is opened.

The checkout uses the same token that opens the pull request, so the branch push and the pull request
come from a single identity. A step `if` cannot read secrets, so which token won is resolved into the
`HAS_SYNC_TOKEN` env var and branched on in the script.

## `ios-interop-verify.yml` — on-demand macOS/Xcode access

`gradle.yml` already builds and tests every Apple target on every pull request, but that matrix only
runs on push and pull request and only reports pass/fail — it does not give a maintainer or an agent
a way to pull real, inspectable build output (framework headers, toolchain versions) on demand. This
workflow exists for that: a `workflow_dispatch`-only job on a `macos-latest` runner, so interop
questions can be answered without a maintainer sitting at a physical Mac and without paying for a
persistent macOS environment.

Cost control: it never runs automatically, GitHub-hosted macOS runner minutes are free for this
public repository (same as the `macos-latest` jobs in `gradle.yml`), and the runner is released as
soon as the job finishes — there is no standing cost.

It also builds and runs `samples/swift-console`. Keeping that sample compiling is no longer this
workflow's job — `gradle.yml`'s `swift-console` matrix entry does that on every push and pull
request, where a regression can still be caught before a release. It is repeated here so an
on-demand run answers an interop question against the same artifact a consumer gets, with the header
dump above it. The interop research probes (`Result<Iban>` erasure, the `IbanParseException`
hierarchy, undeclared-throw behaviour) and the Swift Export experiment live on the
`swift-export-playground` branch, with their findings recorded in
[9-swift-interop-review.md](9-swift-interop-review.md).

## `scorecard.yml` — OpenSSF Scorecard

Scorecard scores a repository against the checks the OpenSSF publishes — branch protection, pinned
dependencies, token permissions, signed releases, whether a CI test workflow exists and runs, and so
on — and files what it finds as code scanning alerts. Two audiences for that here: consumers in
regulated environments increasingly look the score up before taking a dependency, and the checks
themselves are a maintenance checklist for a repository heading to 1.0. See
[160-supply-chain-posture.md](160-supply-chain-posture.md).

`publish_results: true` sends the result to the OpenSSF's public API, which is what backs the badge
in `README.md` and what a consumer querying deps.dev or the Scorecard API reads. It works only from
the default branch of a public repository, which is why there is no `pull_request` trigger: a fork's
run could neither publish nor upload SARIF, and the score is a property of `main` rather than of a
proposed change. The `branch_protection_rule` trigger is there because the Branch-Protection check
reads settings rather than files, so it can go stale without a commit.

## `.github/actions/cache-konan`

Every job that compiles a native target, and every job that runs Dokka, needs the Kotlin/Native
distribution: Dokka runs the Kotlin analysis frontend rather than scraping comments, so it resolves
`nativeMain`/`appleMain` too and pulls the platform klibs in through
`:kiban:downloadKotlinNativeDistribution`. Without this step that is a fresh
`download.jetbrains.com` fetch per job.

The cache key is keyed on `gradle/libs.versions.toml` because the `kotlin` version declared there is
what decides which `kotlin-native-prebuilt-*` bundle gets downloaded. An earlier key —
`hashFiles('**/.lock')` — matched no tracked file, and `hashFiles` returns an empty string when
nothing matches, so it collapsed to a constant `Linux-`/`macOS-` that was never invalidated: after a
Kotlin bump the stale bundle was restored and the new one downloaded on top, growing the entry every
time. The `restore-keys` prefix is what makes a version bump miss the exact key and still restore the
most recent entry for the runner, so the new bundle downloads against a warm cache instead of an
empty one.

## `.github/dependabot.yml`

Two ecosystems, because they are the two this repository uses: the Gradle version catalog and the
actions the workflows depend on.

This does not replace the `nl.littlerobots.version-catalog-update` plugin configured in the root
build script. The two solve the same problem from opposite directions and both are wanted: Dependabot
keeps the catalog from drifting between releases, one pull request at a time, while
`./gradlew versionCatalogUpdate` stays the bulk sweep run deliberately at the start of a development
cycle.

Dependabot reads `gradle/libs.versions.toml` directly, so every catalog entry that resolves to a real
module is covered without extra configuration. The `# @keep` versions (`android-compileSdk`,
`android-minSdk`, `java-version`, `kotlin-version`) are not dependency declarations and are left
alone.

Two things to know about the pull requests this produces:

* A Kotlin bump can change klib ABI rendering, so such a pull request may fail `checkKotlinAbi`
  until someone runs `./gradlew updateKotlinAbi` on the branch. Dependabot cannot finish those on
  its own.
* AGP (`com.android.kotlin.multiplatform.library`) is published only to Google's Maven repository —
  it is not on Maven Central, and the Gradle Plugin Portal just redirects there. So AGP coverage
  depends entirely on Dependabot honouring the `google()` declarations in `settings.gradle.kts`.
  That is expected to work but has not been observed on this repository yet. If AGP is still sitting
  at an old version after a few runs while other entries move, this note should be replaced by an
  explicit one saying AGP stays manual, rather than leaving a false sense of coverage.

Patch and minor bumps are batched into a single pull request because every pull request runs the full
check matrix, several jobs of it on macOS runners. Major bumps still arrive one per pull request,
where they get the individual review they need.
