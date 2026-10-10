# #207: Intel targets — `iosX64` stays, `macosX64` does not come back

0.5.0 dropped `macosX64` while `iosX64` is still published, and #207 reads that as an
inconsistency to settle before the 1.0 freeze: treat the two Intel targets the same, either by
reinstating `macosX64` or by dropping `iosX64` too and calling the whole Apple set Apple silicon
only.

**Decision: neither. The target set stays exactly as it is.** The two targets look like a matched
pair and are not one: `macosX64` is deprecated in the Kotlin Gradle plugin and slated for removal,
`iosX64` is not. The rule the target set already follows is "publish what the Kotlin/Native
toolchain supports", not "publish one architecture per platform" — so there is no inconsistency to
fix, only a rule that was never written down. This document writes it down, and
[VERSIONING.md](../VERSIONING.md) now carries it as policy.

## The two targets are not comparable

#207's premise is that "for a dependency-free, pure-Kotlin library a native target costs nothing
beyond compile minutes and a klib in the publication". That is true of `iosX64`. It is not true of
`macosX64`, because the cost is not in building it — it is in having to remove it again.

In the Kotlin Gradle plugin the target functions are annotated, and exactly four of them are
deprecated:

```
$ javap -p -v org/jetbrains/kotlin/gradle/dsl/KotlinTargetContainerWithPresetFunctions.class
...
  public abstract ... KotlinNativeTargetWithHostTests macosX64();
    Deprecated: true
    RuntimeVisibleAnnotations:
        kotlin.Deprecated(
          message="Target will be removed in a future release. See: https://kotl.in/native-targets-tiers"
          level=Lkotlin/DeprecationLevel;.WARNING
        )
```

Against the plugin this repository builds with (2.4.20), the deprecated set is `linuxArm32Hfp`,
`macosX64`, `tvosX64` and `watchosX64`. `iosX64` carries no annotation at all, and neither does any
other target declared in `library/build.gradle.kts`.

That is the whole story of 0.5.0's removal, and the CHANGELOG said so at the time: `macosX64`,
`tvosX64` and `watchosX64` went away together because JetBrains deprecated the three of them in
Kotlin 2.3.20, and "`iosX64` is not part of that deprecation and stays". The architecture was
never the criterion — the Intel iOS *simulator* is a target JetBrains still supports, and the
Intel *Mac* is one it has deprecated. Reading the removal as "Apple silicon only" is what makes
the remaining `iosX64` look out of place.

## Why not reinstate `macosX64`

Three reasons, in order of how much they cost:

1. **It plants a guaranteed major-version break.** JetBrains says the target will be removed. When
   it is, kiban has to drop it, and [VERSIONING.md](../VERSIONING.md) already commits to treating
   that as a major change "including where the removal is forced from outside". Adding the target
   back now means choosing, deliberately, to owe the 1.x line a 2.0 on JetBrains' schedule — in
   exchange for a target that was already published once and removed once. #207's own reasoning
   points the same way: adding a target is additive and dropping one is a break, so the target to
   avoid is the one whose removal is already announced.
2. **Nothing could test it.** Every macOS job in `.github/workflows/gradle.yml` runs on
   `macos-latest`, which is Apple silicon; there is no Intel macOS runner in the matrix and
   GitHub's hosted fleet is not where one would come from. `checkKotlinAbi` would compile the klib
   from Linux, as it does for every Apple target, and that is all the coverage the target would
   ever get. A published target that no job executes is worse than an absent one.
3. **It makes the build warn, permanently.** `macosX64()` is a deprecated call, so declaring the
   target puts this in every configuration of the build for as long as it is declared:

   ```
   w: library/build.gradle.kts:116:5: 'fun macosX64(): KotlinNativeTargetWithHostTests' is deprecated. Target will be removed in a future release. See: https://kotl.in/native-targets-tiers.
   ```

   That is noise in every log, and it dulls the signal when the same warning lands on a
   *currently published* target — the one case where it has to be read.

The symmetry argument also proves more than it means to. If a native target costs nothing, then
`tvosX64` and `watchosX64` — removed in the same release, for the same reason — have exactly as
good a claim to be reinstated. Nobody is proposing that, which is the tell that the real rule is
the deprecation rather than the architecture.

## Why not drop `iosX64`

Dropping it would buy symmetry and pay for it with a break, which is the wrong direction for a
target the toolchain fully supports.

`iosX64` is the iOS simulator slice for an Intel Mac. Its consumer is not an Intel iPhone — there
is no such thing — but a developer on an Intel Mac building an iOS app and running it in the
simulator. That machine cannot use `iosSimulatorArm64`, so removing `iosX64` makes kiban
unresolvable for that build: a real, non-cosmetic break, charged to a target that costs a klib and
some compile minutes and that JetBrains still supports. "Apple silicon only" would also become a
claim the publication does not actually make, since `iosX64` is where it is false.

## The rule, going forward

> The published target set is whatever the Kotlin/Native toolchain supports. A target enters when
> it becomes available and leaves when JetBrains deprecates it — it is never added or removed for
> consistency with a sibling architecture.

Two consequences worth stating, because they are what makes the rule cheap to follow:

- **A deprecated target is never added.** Not one deprecated at the time, and not one that would
  be deprecated by the plugin version the next release builds with. The deprecation warning on the
  build script is the signal; `javap` on `KotlinTargetContainerWithPresetFunctions` above is how to
  check without guessing.
- **A deprecation on a published target is a scheduled major.** The warning appears before the
  removal does, which is the window in which to write the CHANGELOG and MIGRATION entries. Pre-1.0
  it cost a minor, as 0.5.0 did; from 1.0 the same event costs a major, and the policy already
  says so.

Nothing in `library/build.gradle.kts` changes as a result of this decision. What changes is that
the next person to notice `iosX64` sitting next to `macosArm64` finds the reason written down
instead of inferring a rule from the shape of the list.
