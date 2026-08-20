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
   ./gradlew jvmTest apiCheck ktfmtCheck
   ```

   The full target matrix (Apple, Android, Windows, Linux/Native) runs in CI on the PR via
   `.github/workflows/gradle.yml`; let that go green before merging.

## Tag and publish

5. **Create the GitHub release.** After the release-prep PR is merged, create a GitHub release
   whose tag is `vX.Y.Z` targeting the merge commit on `main`. Mark it as a pre-release if the
   version is a `0.x` or otherwise not a stable release; use the CHANGELOG section as the release
   notes.

6. **Watch the publish workflow.** Creating the release triggers `publish.yml`:
   `verify-version` → the full check matrix → `publishToMavenCentral` (every target, on macOS) →
   the Dokka API docs deploy to GitHub Pages. If `verify-version` fails, the tag and the project
   version disagree — fix the version on `main` (step 1), delete the release/tag, and start again
   from step 5.

7. **Confirm the artifact.** Once the workflow is green, confirm the version appears on
   [Maven Central](https://central.sonatype.com/artifact/nl.bijdorpstudio.kiban/kiban) (it can take
   a while to index) and that the API docs on GitHub Pages reflect the release.

## After the release — open the next cycle

8. **Reopen the CHANGELOG.** Add a fresh `## X.Y.(Z+1) (unreleased)` section (or the next minor/
   major, as appropriate) at the top of `CHANGELOG.md` to collect subsequent entries.

9. **Bump the development version.** Set `version` in `library/build.gradle.kts` to the next
   version so `main` no longer claims the just-released one.

   The README install snippets are **not** touched here: they stay pointed at the version that was
   just released, which is the latest thing a consumer should depend on, not what `main` is building
   towards.

Steps 8 and 9 are the "open the next development cycle" change and can go in as their own small PR.
