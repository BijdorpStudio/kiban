# Versioned API docs

Issue [#162](https://github.com/BijdorpStudio/kiban/issues/162). This records how the Dokka
versioning plugin is wired into the release, and why the archive of older versions is stored the way
it is — the parts that are not obvious from reading `publish.yml`.

## The problem

The `docs` job in `publish.yml` generated the Dokka HTML for the version being released and deployed
it to GitHub Pages, replacing whatever was there. One version is served at a time, always the most
recent one. Before 1.0 that is fine: there are no maintenance lines and nobody is deliberately
staying behind. After it, a consumer still on 1.2 opening the API docs gets 2.0's, with no way to
reach the reference for what they actually depend on.

## What was adopted

Dokka's [versioning plugin](https://kotl.in/dokka-versioning-plugin), version-locked to the Dokka
Gradle plugin in `gradle/libs.versions.toml`, added to `:library`'s `dokkaPlugin` configuration and
configured in `library/build.gradle.kts`:

* The version being generated stays at the **root** of the site, so the Pages URL in the README
  badge keeps landing on the latest docs. Nothing external has to be re-pointed per release.
* Every other version is copied to `older/<version>/`, and the plugin renders a dropdown that
  navigates between them.
* The version label comes from `project.version`. The `verify-version` job has already established
  that the release tag and the project version agree, so it is the version being released.

The one thing the plugin needs and Gradle cannot produce on its own is the set of previous versions.
It reads them from `olderVersionsDir`, which is fed by the `kiban.previousDocVersions` property.

The property is **optional by construction**: it is a `Provider<Directory>` derived from
`providers.gradleProperty(...)`, so with the property unset the `DirectoryProperty` simply has no
value and Dokka generates exactly the single-version site it did before. Two things depend on that:
a local `./gradlew :library:dokkaGeneratePublicationHtml` needs no archive to work, and the first
release after this landed has no archive to give it.

## Where the archive lives, and why not in this repository

The archive is the previously published site. It is stored on a **`docs-archive` branch**, one
directory per version under `versions/`, and it is not merged into `main` in any form — a full Dokka
HTML site for a multiplatform library is tens of megabytes, and one per release in the history of
`main` would dominate every clone of this repository forever.

The alternatives that were rejected:

* **Regenerating older versions from their tags at release time.** Fully reproducible and needs no
  storage at all, but it means one full Dokka run per historical version on every release, on a
  project whose Dokka run resolves Kotlin/Native source sets. The cost grows with every release and
  buys nothing that a stored copy does not.
* **Reading back the previous run's Pages artifact.** Workflow artifacts expire (90 days on a public
  repository). An archive that silently loses versions when releases are more than a quarter apart
  is worse than no archive.
* **Fetching the live Pages site over HTTP.** There is no listing to enumerate, so this means
  mirroring a site by crawling its links — fragile in a way that would only ever be discovered
  during a release.

## How the branch is maintained

The `archive-docs` job in `publish.yml` runs after the deployment and archives **the Pages artifact
that was just deployed** rather than re-generating anything, so what ends up behind the dropdown is
byte for byte what was served as that version.

It reconstructs the whole branch from that artifact each release, which works because the deployed
site is already the complete set: the root is the version just published, and `older/` holds every
version before it. So `versions/` becomes `older/*` carried over unchanged, plus the root as
`versions/<new version>`.

What identifies an archived version is the `version.json` the plugin writes at the root of every
site it generates — not the directory name. A directory without one is skipped with a warning
(`Failed to find versions file named version.json in ...`) and silently misses from the dropdown.
Nothing has to be done to satisfy that here, because what gets archived is a generated site root,
which always carries one; it is worth knowing when reading the job, since it is what makes stripping
`older/` from that root safe.

Two details are deliberate:

* **The new version's own `older/` copy is dropped before archiving.** Without that, every future
  version would carry a nested copy of every past one and the archive would double in size per
  release.
* **The branch is force-pushed as a fresh single commit**, not committed onto its own history.
  Nothing reads its history — the `docs` job only ever checks out the tip — and a real history would
  keep every past release's copy of the whole site alive as git objects.

`contents: write` is what pushing a branch takes, and it is the reason this is a separate job at
all: that permission must not sit on a job that runs the Gradle build, where any plugin's or
dependency's build logic would inherit it. `archive-docs` runs no project code.

## Operational notes

* **Dropping a version from the archive** is a matter of deleting its directory from the
  `docs-archive` branch. The next release regenerates the branch from what the site then serves, so
  the deletion sticks without any further step.
* **Losing the branch** costs the archive, not the docs: the `docs` job's restore step is
  `continue-on-error`, so a missing branch produces a single-version site and the archive starts
  again from that release.
* **Nothing here runs outside a release.** Pull request CI does not generate docs, so a change to
  this wiring is first exercised by the release that follows it.
