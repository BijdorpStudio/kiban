# Notes for agents working in cloud sandboxes

This repo is regularly worked on by automated coding agents running in cloud
sandboxes (cloud CI runners, cloud dev containers, etc.). Those sandboxes have
real limitations that look like build breakage at first glance but aren't.
Read this before spending time diagnosing a `./gradlew` failure as a code
regression.

Network egress is no longer a problem: `./gradlew jvmTest`, the full
`apiCheck` (both `jvmApiCheck` and `klibApiCheck` — unbuildable Apple targets
are inferred), `ktfmtCheck`, Kotlin/Native compilation and testing for host
targets (`compileKotlinLinuxX64`, `linuxX64Test`), and `:samples:jvm-cli:run`
all run out of the box. (Older revisions of this file documented egress
blocks on `dl.google.com` and `download.jetbrains.com` that broke AGP
resolution and the Kotlin/Native toolchain download, with workarounds to
match; those restrictions have been lifted and the workarounds are gone.)
The two gaps that remain are about what's installed on the container, not
the network.

## No Apple toolchain in most cloud sandboxes

Targets that need Xcode/macOS — `ios*`, `macos*`, `tvos*`, `watchos*` — can't
be compiled or tested on a Linux container, and neither can
`assembleKibanDebugXCFramework` (used by `samples/swift-console`, see #68).
`apiCheck` is *not* blocked by this: the klib check infers the Apple targets
from the buildable ones and passes here.

This also blocks anything that requires actually running Swift-facing code
(e.g. reviewing how the API surfaces through Objective-C interop or Swift
Export) — that needs a real macOS/Xcode host, not speculation from reading
Kotlin source.

`.github/workflows/ios-interop-verify.yml` provides on-demand access to that
host: a `workflow_dispatch`-only job on a `macos-latest` runner (Actions tab →
"iOS/Swift interop verification" → "Run workflow"), for pulling real build
output and toolchain versions without a maintainer at a physical Mac. It now
also builds and runs `samples/swift-console` (#68) — the concrete testbed
#9's actual API review needs — so Swift Export artifact generation for that
sample is the only piece #9 still has to add.

## Android tasks need an SDK the sandbox doesn't have

Android-*specific* tasks (`:library:testAndroidHostTest`,
`assembleAndroidMain`, lint) fail with "SDK location not found. Define a
valid SDK location with an ANDROID_HOME environment variable". That's a
missing Android SDK installation, not a network or code problem. JVM tasks
are unaffected — the Android target's presence in the build breaks nothing
else.

## What "verified" should mean when local verification is incomplete

Don't claim untested changes pass. `jvmTest`, the full `apiCheck`,
`ktfmtCheck` and the Linux-host Kotlin/Native tasks all run here and count
as real verification. Apple-target compilation/tests and Android-specific
tasks don't run here — say explicitly in the PR body which commands you ran
and which targets were left unverified. `.github/workflows/gradle.yml` runs
the full target matrix across Linux and macOS runners — that's the actual
verification once the PR is pushed, and `ios-interop-verify.yml` covers
anything Swift-facing on demand.
