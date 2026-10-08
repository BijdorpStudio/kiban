# Notes for agents working in cloud sandboxes

This repo is regularly worked on by automated coding agents running in cloud
sandboxes (cloud CI runners, cloud dev containers, etc.). Those sandboxes have
real limitations that look like build breakage at first glance but aren't.
Read this before spending time diagnosing a `./gradlew` failure as a code
regression.

Network egress is no longer a problem: `./gradlew jvmTest`, `ktfmtCheck`,
Kotlin/Native compilation and testing for host targets
(`compileKotlinLinuxX64`, `linuxX64Test`), and `:samples:jvm-cli:run`
all run out of the box. `checkKotlinAbi` needs one install step first — see
below; it no longer runs without an Android SDK. (Older revisions of this file documented egress
blocks on `dl.google.com` and `download.jetbrains.com` that broke AGP
resolution and the Kotlin/Native toolchain download, with workarounds to
match; those restrictions have been lifted and the workarounds are gone.)
The two gaps that remain are about what's installed on the container, not
the network.

## No Apple toolchain in most cloud sandboxes

Apple targets — `ios*`, `macos*`, `tvos*`, `watchos*` — can't be *tested* on a
Linux container, and `assembleKibanDebugXCFramework` (used by
`samples/swift-console`, see #68) can't be built there either: linking a
framework or an executable is what needs Xcode.

Compiling them to a klib does not. `compileKotlinIosArm64` and its siblings run
here, which is why `checkKotlinAbi` covers every Apple target from a Linux
container rather than inferring them (#182).

This also blocks anything that requires actually running Swift-facing code
(e.g. reviewing how the API surfaces through Objective-C interop or Swift
Export) — that needs a real macOS/Xcode host, not speculation from reading
Kotlin source.

`.github/workflows/ios-interop-verify.yml` provides on-demand access to that
host: a `workflow_dispatch`-only job on a `macos-latest` runner (Actions tab →
"iOS/Swift interop verification" → "Run workflow"), for pulling real build
output and toolchain versions without a maintainer at a physical Mac. It also
builds and runs `samples/swift-console` (#68) — the concrete testbed #9's
actual API review needs — so Swift Export artifact generation for that sample
is the only piece #9 still has to add.

Keeping that sample compiling is not on-demand work, though: `gradle.yml`'s
`swift-console` matrix entry assembles the XCFramework and runs the sample on
every push and pull request (#155). So a change that breaks Objective-C
interop fails CI on the pull request that made it — you still cannot reproduce
that here, but you will not learn about it during a release either.

## Android tasks need an SDK the sandbox doesn't ship

Android-*specific* tasks (`:kiban:testAndroidHostTest`,
`assembleAndroidMain`, lint) fail with "SDK location not found. Define a
valid SDK location with an ANDROID_HOME environment variable". That's a
missing Android SDK installation, not a network or code problem. JVM tasks
are unaffected — the Android target's presence in the build breaks nothing
else.

`checkKotlinAbi` is *not* unaffected, as of Kotlin 2.4.20: the android target
now gets its own dump (`kiban/api/android/kiban.api`), dumping it compiles
`androidMain`, and so the task fails on the same missing-SDK message before it
compiles anything. Earlier Kotlin versions dumped only the jvm and klib
surfaces, which is why older revisions of this file called the full ABI check
an out-of-the-box task.

Since egress is open, the SDK is an install rather than a blocker — only the
platform the build compiles against is needed, and no emulator or NDK:

```sh
curl -o /tmp/cmdline-tools.zip \
  https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
mkdir -p "$HOME/android-sdk/cmdline-tools"
unzip -q /tmp/cmdline-tools.zip -d "$HOME/android-sdk/cmdline-tools"
mv "$HOME/android-sdk/cmdline-tools/cmdline-tools" "$HOME/android-sdk/cmdline-tools/latest"
export ANDROID_HOME="$HOME/android-sdk"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "platforms;android-36" "build-tools;36.0.0"
```

`android-36` is `android-compileSdk` in `gradle/libs.versions.toml`; match it
rather than hardcoding a version that will drift. With `ANDROID_HOME` exported,
`checkKotlinAbi` and the Android-specific tasks above all pass here — so the
Android target is verifiable in a sandbox now, and an Android-specific failure
is worth installing the SDK to reproduce rather than deferring to CI.

## What "verified" should mean when local verification is incomplete

Don't claim untested changes pass. `jvmTest`, `ktfmtCheck` and the Linux-host
Kotlin/Native tasks all run here and count as real verification, as do the full
`checkKotlinAbi` and the Android-specific tasks once the SDK above is
installed. Apple-target *tests*, the XCFramework and the Swift sample don't run
here, and `jsTest`/`wasmJsTest` may not either: Kotlin's Karma setup resolves a
dependency straight from a GitHub repository, which the sandbox's HTTPS proxy
can break (`Parse Error: Data after 'Connection: close'`) — a proxy limitation,
not a test failure. Say explicitly in
the PR body which commands you ran and which targets were left unverified.
`.github/workflows/gradle.yml` runs the full target matrix across Linux and
macOS runners, `samples/swift-console` included — that's the actual
verification once the PR is pushed, and `ios-interop-verify.yml` covers
anything Swift-facing that needs inspectable output on demand.
