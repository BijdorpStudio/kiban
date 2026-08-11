# Notes for agents working in cloud sandboxes

This repo is regularly worked on by automated coding agents running in cloud
sandboxes (cloud CI runners, cloud dev containers, etc.). Those sandboxes have
real limitations that look like build breakage at first glance but aren't.
Read this before spending time diagnosing a `./gradlew` failure as a code
regression.

The JVM side works normally: plain `./gradlew jvmTest`, `jvmApiCheck`,
`ktfmtCheck`, `:samples:jvm-cli:run` and friends run out of the box, with no
workarounds. (Older revisions of this file documented an egress block on
`dl.google.com` that broke every `./gradlew` invocation at configuration time
and required temporarily removing the Android target; that restriction has
been lifted, and the workaround is gone with it.) The gaps that remain are
below.

## No Apple toolchain in most cloud sandboxes

Targets that need Xcode/macOS — `ios*`, `macos*`, `tvos*`, `watchos*` — can't
be compiled or tested on a Linux container. `apiCheck` builds a klib for
*every* declared target (see `library/build.gradle.kts`'s target list), so it
also needs the Apple toolchain and can't fully pass here.

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

## Kotlin/Native toolchain dependencies can't download

Any task that needs to *compile* a Kotlin/Native target — not just Apple
ones — still fails here. The compiler distribution itself resolves fine (it
comes from Maven Central), but the platform toolchain bundles it then fetches
(sysroots, gcc toolchains) download from `download.jetbrains.com`, which the
sandbox's egress policy still blocks. The failure looks like:

```
> Task :library:downloadKotlinNativeDistribution FAILED
Cannot download a dependency https://download.jetbrains.com/kotlin/native/...:
java.io.IOException: Unable to tunnel through proxy. Proxy returns "HTTP/1.1 403 Forbidden"
```

after ten retries. This hits even targets this Linux host could otherwise
build natively (`linuxX64`/`linuxArm64`), and through them `apiCheck` (its
klib part), `publishToMavenLocal`, and `assembleKibanDebugXCFramework` (used
by `samples/swift-console`, see #68). `./gradlew jvmTest` is unaffected: the
JVM target never touches the Kotlin/Native compiler. Don't route around it
(no mirrors, no disabling targets in a committed change) — disclose it in the
PR instead.

## Android tasks need an SDK the sandbox doesn't have

Android-*specific* tasks (`:library:testAndroidHostTest`,
`assembleAndroidMain`, lint) fail with "SDK location not found. Define a
valid SDK location with an ANDROID_HOME environment variable". That's a
missing Android SDK installation, not a network or code problem. JVM tasks
are unaffected — the Android target's mere presence in the build no longer
breaks anything.

## What "verified" should mean when local verification is blocked

Don't claim untested changes pass. `jvmTest` and `jvmApiCheck` run fully
here; Kotlin/Native compilation, the klib side of `apiCheck`, Apple targets,
and Android-specific tasks don't. Say explicitly in the PR body which
commands you ran and which targets were left unverified.
`.github/workflows/gradle.yml` runs the full target matrix across Linux and
macOS runners with unrestricted network access — that's the actual
verification once the PR is pushed.
