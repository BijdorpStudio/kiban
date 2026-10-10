# #116 spike: adopt Develocity (build scans / build caching)?

This is the evaluation #116 asked for: decide between doing nothing, publishing free build scans
from CI, and applying to the [Develocity OSS program](https://github.com/gradle/develocity-oss-projects).

**Recommendation: publish free public build scans, from CI only, wired through the
`gradle/actions/setup-gradle` inputs the workflows already use — not through the
`com.gradle.develocity` settings plugin. Do not pursue the OSS program.** The settings plugin
works on this repo's exact toolchain, including under the configuration cache, and it fails soft
when the backend is unreachable — so the sandbox worry in the issue is unfounded. It is still the
wrong mechanism here, for two reasons the probe turned up: its version cannot come from the
version catalog, and applying it in `settings.gradle.kts` opts *every* build in, including local
and agent-sandbox ones that have nothing to publish to. The action does the same job in three
lines of YAML, on CI only, with no build-file change at all.

Everything below was measured against this repo's actual toolchain (Gradle 9.6.1, Kotlin 2.4.10,
Develocity Gradle plugin 4.5.0 — the current release) by really applying the plugin and running
builds. The probe edits to `settings.gradle.kts` and `gradle/libs.versions.toml` were reverted
before committing; the observations they produced are quoted verbatim. Where a claim could not be
verified from this sandbox it is called out inline rather than papered over.

## 1. The settings plugin applies cleanly, and is configuration-cache compatible

`4.5.0` is the current release (`com.gradle.develocity.gradle.plugin/maven-metadata.xml`,
`lastUpdated 20260630`). Applied in `settings.gradle.kts` after the existing `pluginManagement`
block:

```kotlin
plugins {
    id("com.gradle.develocity") version "4.5.0"
}
```

`./gradlew help` → `BUILD SUCCESSFUL in 31s`, and on the following run:

```
Configuration cache entry reused.
```

That is the ordering question in the issue answered: the `plugins {}` block goes after
`pluginManagement {}` and before `dependencyResolutionManagement {}`, and nothing in this
build — `org.gradle.configuration-cache=true`, `org.gradle.parallel=true`,
`org.gradle.caching=true` all on — objects. No deprecation warnings, no configuration-cache
problems reported.

## 2. Publishing fails soft — the sandbox concern is unfounded

This is the concern #116 flagged as a possible blocker, and it is worth recording precisely
because the answer is "no problem" rather than "needs a workaround".

This sandbox cannot reach the scan backend at all. The egress proxy rejects the tunnel outright:

```
$ curl -v https://scans.gradle.com/
> CONNECT scans.gradle.com:443 HTTP/1.1
< HTTP/1.1 403 Forbidden
* CONNECT tunnel failed, response 403
```

To measure what a build does when the backend is unreachable, the probe pointed the plugin at a
deliberately unreachable loopback endpoint (`server = "https://127.0.0.1:1"`), which exercises the
same failure path without sending anything off the machine and without accepting anyone's terms
of use. The result:

```
BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Publishing Build Scan to Develocity...
Publishing Build Scan failed due to network error 'java.net.ConnectException: Connection refused' (2 retries remaining)...
Publishing Build Scan failed due to network error 'java.net.ConnectException: Connection refused' (1 retry remaining)...
A network error occurred.
...
Configuration cache entry stored.
EXIT=0
```

Three things to take from that. The build **succeeds** — publishing runs after the build result is
decided and a failure to publish is not a build failure, so no `develocity { buildScan {
publishing.onlyIf { … } } }` guard is needed to keep sandboxes green. The configuration cache
entry is still stored, so the failure does not poison the cache either. And the publish attempt
retries three times: here each attempt was refused instantly, but against a host that blackholes
the connection rather than refusing it, three connect timeouts would be added to the tail of
every build. That last point is a cost paid by whoever is *not* on CI, which is the next section's
argument.

## 3. Why not the settings plugin, then: the version catalog will not hold it

The issue asks about "interaction with the settings plugin ordering and the version catalog".
Ordering is fine (section 1). The catalog is not — the version cannot live there. Adding
`develocity = { id = "com.gradle.develocity", version.ref = "develocity-plugin" }` to
`gradle/libs.versions.toml` and referring to it the way every other plugin in this repo is
referred to fails:

```
e: file:///home/user/kiban/settings.gradle.kts:10:11: Unresolved reference 'libs'.
e: file:///home/user/kiban/settings.gradle.kts:10:24: Unresolved reference 'develocity'.
```

The catalog is created by `dependencyResolutionManagement` *in the same file*, so it does not
exist yet when the settings `plugins {}` block is evaluated. The version therefore has to be a
hardcoded string literal in `settings.gradle.kts`, which puts it outside
`./gradlew versionCatalogUpdate` — the bulk sweep that `build.gradle.kts` configures and that the
`dependabot.yml` comment describes as one half of this repo's update strategy. Dependabot would
still cover it (its Gradle file fetcher lists `SUPPORTED_SETTINGS_FILE_NAMES = %w(settings.gradle
settings.gradle.kts)`, and the version would be a string literal), so the entry would not go
stale — but it would be the single build-tooling version in the repo that one of the two
mechanisms cannot see. That is a small, permanent wart in exchange for nothing, because:

## 4. The action already does this, CI-only, with no build-file change

All four workflows (`gradle.yml`, `publish.yml`, `registry-sync.yml`, `ios-interop-verify.yml`)
already run `gradle/actions/setup-gradle@v6`. That action publishes build scans on its own,
injecting the plugin without touching the build:

```yaml
- name: Setup Gradle to publish build scans
  uses: gradle/actions/setup-gradle@v6
  with:
    build-scan-publish: true
    build-scan-terms-of-use-url: 'https://gradle.com/terms-of-service'
    build-scan-terms-of-use-agree: 'yes'
```

This is strictly better than the settings plugin for kiban:

- **No build-file change.** `settings.gradle.kts` and the version catalog stay exactly as they
  are, so section 3's wart disappears entirely.
- **CI-only by construction.** Local developer builds and cloud-sandbox agent builds
  (see [`CLAUDE.md`](../CLAUDE.md)) never load the plugin, so they never attempt a publish, never
  pay the retry tail from section 2, and never leak a developer's machine details to a public
  dashboard.
- **Reversible in one commit** that touches only workflow YAML.

The one thing it gives up is `--scan` on a local build, which is exactly the thing that should not
be on by default here.

Two caveats that this sandbox cannot settle, both of which the first CI run will:
`build-scan-publish` injects the plugin through an init script rather than the settings file, and
the interaction of that injection with this build's configuration cache was not verified here (the
directly-applied plugin is fine — section 1). And `gradle.yml` pipes Gradle's output through
`tee`; the scan URL is printed to stdout, so it should land in both the console and
`gradle-build.log`, but that has not been observed.

## 5. Remote build cache: nothing worth chasing

The issue guesses the caching gains are modest. They are smaller than that.

The whole library is 1,509 lines of `commonMain` across 5 files, plus 1,891 lines of
`commonTest`. A from-scratch `:library:jvmTest` — `--rerun-tasks`, so nothing up-to-date and
nothing from the local cache — is:

```
BUILD SUCCESSFUL in 1m 26s
5 actionable tasks: 5 executed
```

Five tasks, and most of that wall-clock is Kotlin compiler and daemon startup rather than work a
cache could hand back. Against that, a remote cache has to win against three layers that already
exist: `org.gradle.caching=true` (local cache), `setup-gradle`'s Gradle User Home cache — which
`gradle.yml` already tunes with `cache-read-only` and an encryption key — and the dedicated
`.github/actions/cache-konan` action added in #109.

The structural point is worse for remote caching than the size one. `gradle.yml` fans out over 14
matrix entries, but they are 14 *different targets* — `iosSimulatorArm64Test`, `macosArm64Test`,
`tvosSimulatorArm64Test`, `watchosSimulatorArm64Test`, `jvmTest`, `linuxX64Test`, `jsTest`,
`wasmJsTest`, cross-compile, `apiCheck`, Android, Dokka, ktfmt, and the JVM sample. Compilation
outputs are keyed per target, so there is almost nothing for one job to hand to another; the
shared common-metadata compilation is the exception, and it is a small fraction of a 1m26s build.
Remote caching pays off for large multi-module builds where many jobs redo the same work. This is
the opposite shape.

kiban is also a public repository, so Actions minutes are free — there is not even a billing
argument to fall back on.

## 6. What build scans add over what CI already uploads

Worth being honest that the issue's stated win — "diagnosing CI failures across the Linux/macOS
matrix without re-running jobs" — is *already partly solved*. `gradle.yml` tees every build to
`build/reports/ci/gradle-build.log`, uploads `**/build/reports/**`, `**/build/test-results/**` and
`**/build/api/**` on failure with 7-day retention, and even regenerates a ktfmt patch when
`ktfmtCheck` fails. Cold-diagnosing a red matrix entry does not currently require re-running it.

What a scan adds on top is real but narrower than the issue implies: a per-task timeline (which of
the 14 entries is actually slow, and why), dependency-resolution detail, and a shareable URL that
can be pasted into a PR thread instead of "download this artifact zip and unpack it". For a
14-job matrix where the macOS entries dominate wall-clock, the timeline is the genuinely new
information.

That is a modest but real win for a three-line YAML change with an instant undo. It is not a win
worth a build-file change, a catalog exception, and a retry tail on every sandbox build — which is
the whole argument for section 4 over section 1.

## 7. What gets uploaded, and the one thing this document cannot decide

`gradle.com` and `docs.gradle.com` are both blocked from this sandbox, so the published
"what data is captured" page could not be read and is deliberately not summarised here from
memory. What can be said from the repo's own side: publishing is CI-only under this
recommendation, so everything uploaded originates on an ephemeral public GitHub runner building a
public repository — no developer hostnames, usernames, paths or environment. Build scans capture
build structure, task/dependency/test data and console output, not source archives; even so, the
scans would be **public** at scans.gradle.com and should be treated as such.

The decision this document cannot make on the project's behalf is the terms-of-use acceptance.
`build-scan-terms-of-use-agree: 'yes'` is an agreement with Gradle Technologies entered into for
the project, and that is a maintainer's call, not a mechanical one. The follow-up PR should be
merged only if that acceptance is intended.

## 8. The OSS program is not a realistic fit

The [program roster](https://github.com/gradle/develocity-oss-projects) lists ~25 participants:
the Apache Software Foundation, Spring, Kotlin, Quarkus, Micronaut, AndroidX, JUnit,
OpenTelemetry, Testcontainers, Detekt, XWiki and similar. These are ecosystems, not single
libraries. kiban is a 1,509-line library with a 14-job CI matrix that finishes in minutes.

Beyond the eligibility mismatch, the repo itself documents no application process — the
[program agreement](https://gradle.com/legal/gradle-technologies-open-source-program-agreement/)
is executed between Gradle, Inc. and the project, i.e. a legal agreement a maintainer signs, which
is not something a spike or an automated contributor can initiate. And the two things a hosted
instance would add over free scans are remote build caching (section 5: no gain here) and test
analytics/flaky detection (kiban's suite is deterministic country-data assertions; there is no
flakiness problem to detect).

Nothing here is permanent. If kiban's build ever grows into a multi-module shape where cross-job
cache sharing pays, this can be revisited — the free scans recommended above are the prerequisite
data for making that case anyway.

## Verdict

Adopt free public build scans on CI, via `gradle/actions/setup-gradle` inputs. Skip the settings
plugin. Skip the OSS program, and close #116 as answered.

The settings plugin is not rejected because it is broken — it demonstrably works here, config
cache and all, and it fails soft when it cannot publish. It is rejected because the action gets
the same scans with a smaller blast radius: no build-file change, no version-catalog exception,
and nothing at all happening on developer machines or in agent sandboxes.

## Follow-up

The wiring PR is small and should carry:

- `build-scan-publish: true` plus `build-scan-terms-of-use-url` /
  `build-scan-terms-of-use-agree` on the `setup-gradle` step in `.github/workflows/gradle.yml`.
  Decide per workflow whether `publish.yml`, `registry-sync.yml` and `ios-interop-verify.yml`
  want them too — `gradle.yml` is the one with the matrix and therefore the one with the payoff;
  the other three are low-frequency and can stay unscanned.
- No change to `settings.gradle.kts`, `gradle/libs.versions.toml`, or `gradle.properties`.
- Explicit maintainer sign-off on the terms-of-use acceptance (section 7). This is the gate on
  the PR, not the code.
- Verify on the first real CI run, neither of which this sandbox can do (section 4): that scan
  publishing does not disturb the configuration cache when injected via init script, and that the
  scan URL reaches `build/reports/ci/gradle-build.log` through the existing `tee`.
