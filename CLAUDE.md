# Notes for agents working in cloud sandboxes

This repo is regularly worked on by automated coding agents running in cloud
sandboxes (cloud CI runners, cloud dev containers, etc.). Those sandboxes have
real limitations that look like build breakage at first glance but aren't.
Read this before spending time diagnosing a `./gradlew` failure as a code
regression.

## Network egress may be restricted

`library/build.gradle.kts` applies `com.android.kotlin.multiplatform.library`
(AGP) unconditionally — the module always declares an `androidLibrary {}`
target, there's no way to opt out of it per-invocation. That plugin can only
be resolved from Google's Maven repo (`dl.google.com`).

If the sandbox's egress policy blocks `dl.google.com`, **every**
`./gradlew` invocation fails during Gradle's configuration phase, before any
task runs — including plain `./gradlew jvmTest`, not just Android- or
`apiCheck`-specific tasks. The failure looks like:

```
Plugin [id: 'com.android.kotlin.multiplatform.library', version: '...', apply: false] was not found in any of the following sources:
...
    Google
    MavenRepo
    Gradle Central Plugin Repository
```

If you hit exactly this — a plugin-resolution failure against Google's Maven
repo, reproducible on a clean `main` checkout with no changes of yours
involved — it's this sandbox limitation, not a regression. Don't try to route
around it (no mirrors, no repository substitution, no disabling AGP in a
committed change). Disclose it in the PR instead.

**Workaround to still run JVM-only checks locally:** temporarily remove the
Android target so the AGP plugin never needs to resolve, run the checks, then
revert before committing:

1. In `build.gradle.kts`, drop the `alias(libs.plugins.android.kotlin.multiplatform.library) apply false` line.
2. In `library/build.gradle.kts`, drop the `alias(libs.plugins.android.kotlin.multiplatform.library)` line and the `androidLibrary { ... }` block.
3. Run `./gradlew jvmTest` / `./gradlew apiCheck` (the latter will still only cover targets buildable on this host — see below).
4. `git checkout -- build.gradle.kts library/build.gradle.kts` to revert both files before staging anything. The committed diff must never include this workaround.

With the Android target gone, `library/api/`'s expected dump path changes
from nested (`library/api/jvm/library.api`) to flat (`library/api/library.api`),
so `jvmApiCheck`/`apiCheck` fails with a "file does not exist" error against
the real (nested) dump even when the API itself is unchanged. That failure is
an artifact of the workaround, not a real API mismatch — don't act on it (no
regenerating dumps, no restructuring `library/api/`) unless you've also
confirmed a real API change some other way.

## No Apple toolchain in most cloud sandboxes

Targets that need Xcode/macOS — `ios*`, `macos*`, `tvos*`, `watchos*` — can't
be compiled or tested on a Linux container. `apiCheck` builds a klib for
*every* declared target (see `library/build.gradle.kts`'s target list), so it
also needs the Apple toolchain and can't fully pass here even once the AGP
block above is worked around.

This also blocks anything that requires actually running Swift-facing code
(e.g. reviewing how the API surfaces through Objective-C interop or Swift
Export) — that needs a real macOS/Xcode host, not speculation from reading
Kotlin source.

## What "verified" should mean when local verification is blocked

Don't claim untested changes pass. If `jvmTest`/`apiCheck` couldn't be run
(or could only be run with the Android-target workaround above, or could only
cover a subset of targets), say so explicitly in the PR body: which commands
you ran, which workaround (if any) you used, and which targets were left
unverified. `.github/workflows/gradle.yml` runs the full target matrix across
Linux and macOS runners with unrestricted network access — that's the actual
verification once the PR is pushed.
