# Releasing kiban

This is the checklist for cutting a kiban release and publishing it to Maven Central. It exists
because nothing about a release is inferred from the tag alone: `.github/workflows/publish.yml`
fires on any GitHub release `released`/`prereleased` event and publishes whatever the checked-out
commit declares, so the version, the docs and the announcement all have to be set on `main` *before*
the tag is created.

The versioning rules that decide the next number — what counts as major, minor or patch, and why a
registry data update is a minor — live in [VERSIONING.md](VERSIONING.md). This document is the
mechanical sequence; that one is the policy.

## What the tag guard enforces

`publish.yml` runs a `verify-version` job first that compares the release tag (minus a leading `v`)
against `version` in `library/build.gradle.kts` and fails the whole workflow on a mismatch. So the
one thing you cannot get away with is tagging a version the build script does not declare: if you
skip the version bump below, the publish fails fast instead of silently shipping the old version.
Everything else in this checklist is not machine-enforced — follow it.

## The publish approval gate

The `publish` job — the only one holding the Maven Central credentials and the GPG signing key — is
bound to a `release` GitHub environment. Creating the release still starts the workflow, but that
job waits: a required reviewer gets a "Review deployments" prompt on the run, and nothing is signed
or uploaded until one of them approves it. That is the deliberate step between "someone created a
GitHub release" and "an artifact is on Maven Central", and it is what an accidental tag — or a
compromised token that can create releases — runs into.

The environment is a repository setting rather than workflow content, so the binding only gates
anything once it is configured under Settings → Environments → `release`. Until then GitHub creates
the environment implicitly on the first run, with no rules, and the publish proceeds exactly as it
did before.

* **Required reviewers.** At least one, or the job runs unattended and the binding buys nothing
  beyond a deployment record.

* **The publish secrets, moved onto the environment.** `MAVEN_CENTRAL_USERNAME`,
  `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY_ID`, `SIGNING_PASSWORD` and `GPG_KEY_CONTENTS` should be
  environment secrets on `release`, not repository secrets. Environment secrets are readable only by
  a job that declares that environment, so after the move no other job or workflow in the repository
  can reach the signing key at all. (`GRADLE_ENCRYPTION_KEY` is read by the other jobs here too and
  stays a repository secret.)

* **Deployment branches, if you set them at all.** The publish runs on the release tag ref, never on
  a branch, so the "protected branches only" rule would reject every publish. Leave the setting at
  "no restriction", or use "selected branches and tags" with a `v*` tag rule.

## Before you tag (on `main`, via a normal PR)

Prepare all of these in one release-prep pull request and merge it to `main`:

1. **Bump the version.** In `library/build.gradle.kts`, set `version = "X.Y.Z"` to the version you
   are about to release. This is the value the tag guard checks against.

2. **Finalise the CHANGELOG.** In `CHANGELOG.md`, replace the `## X.Y.Z (unreleased)` marker at the
   top with `## X.Y.Z`, so the section reads as released. Make sure it lists every user-facing
   change since the last release, with breaking changes called out first (see the existing
   sections for the shape).

3. **Update the README install snippets.** In `README.md`, bump the two dependency coordinates
   (`implementation("nl.bijdorpstudio.kiban:kiban:X.Y.Z")`) to the version being released. These
   tell a reader what to depend on — the latest *released* version — so they are updated as part of
   the release, not when the development cycle opens. (Historical version mentions in prose, e.g.
   "since 0.5.0", are not install instructions and stay as they are.)

4. **Verify locally.** Run the checks that pass in a cloud sandbox (see [CLAUDE.md](CLAUDE.md)):

   ```
   ./gradlew jvmTest checkKotlinAbi ktfmtCheck
   ```

   The full target matrix (Apple, Android, Windows, Linux/Native) runs in CI on the PR via
   `.github/workflows/gradle.yml`; let that go green before merging.

## Tag and publish

5. **Create the GitHub release.** After the release-prep PR is merged, create a GitHub release
   whose tag is `vX.Y.Z` targeting the merge commit on `main`. Mark it as a pre-release if the
   version is a `0.x` or otherwise not a stable release; use the CHANGELOG section as the release
   notes.

6. **Watch the publish workflow, and approve the publish.** Creating the release triggers
   `publish.yml`: `verify-version` → the full check matrix → `publishToMavenCentral` (every target,
   on macOS) → the Dokka API docs deploy to GitHub Pages. The publish job pauses for the `release`
   environment's required reviewers once the matrix is green — approve it from the run page to let
   it upload (see [The publish approval gate](#the-publish-approval-gate)). If `verify-version`
   fails, the tag and the project version disagree — fix the version on `main` (step 1), delete the
   release/tag, and start again from step 5.

7. **Confirm the artifact.** Once the workflow is green, confirm the version appears on
   [Maven Central](https://central.sonatype.com/artifact/nl.bijdorpstudio.kiban/kiban) (it can take
   a while to index) and that the API docs on GitHub Pages reflect the release — the version just
   released at the root, and the previous ones still reachable from the version dropdown (see
   [Versioned API docs](#versioned-api-docs) below).

   The publish job also records [build provenance](https://docs.github.com/actions/security-for-github-actions/using-artifact-attestations/using-artifact-attestations-to-establish-provenance-for-builds)
   for every artifact it uploaded, listed under the repository's **Attestations** tab. A spot check
   on a downloaded jar is the quickest confirmation that the attestation covers what Maven Central
   is serving:

   ```shell
   gh attestation verify kiban-jvm-X.Y.Z.jar --repo BijdorpStudio/kiban
   ```

## Versioned API docs

The docs deploy is versioned (#162): the release being published lands at the root of GitHub Pages,
so the README badge always points at the latest reference, and every previously published version
stays reachable from the version dropdown.

Nothing in this checklist has to be done to keep that working. The two jobs at the end of
`publish.yml` do it: `docs` restores the previously published versions from the `docs-archive`
branch and generates the site around them, and `archive-docs` puts the version just deployed back
onto that branch for the next release to pick up.

What is worth knowing when reading a release:

* **The archive lives on the `docs-archive` branch**, one directory per version under `versions/`,
  rebuilt as a single commit on every release. It is never merged into `main` — a full Dokka site
  per release would dominate every clone.
* **To drop a version from the dropdown**, delete its directory from that branch. The next release
  regenerates the branch from what the site serves at that point, so the deletion sticks.
* **If the branch is missing** — as it was for the first release after this landed — the `docs` job
  logs "No archived versions restored" and publishes a single-version site. That is not a failure;
  the archive starts from that release onwards.

The full reasoning, including the storage options that were rejected, is in
[docs/162-versioned-api-docs.md](docs/162-versioned-api-docs.md).

## After the release — open the next cycle

8. **Reopen the CHANGELOG.** Add a fresh `## X.Y.(Z+1) (unreleased)` section (or the next minor/
   major, as appropriate) at the top of `CHANGELOG.md` to collect subsequent entries.

9. **Bump the development version.** Set `version` in `library/build.gradle.kts` to the next
   version so `main` no longer claims the just-released one.

   The README install snippets are **not** touched here: they stay pointed at the version that was
   just released, which is the latest thing a consumer should depend on, not what `main` is building
   towards.

Steps 8 and 9 are the "open the next development cycle" change and can go in as their own small PR.
