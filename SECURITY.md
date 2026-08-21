# Security Policy

kiban validates and parses bank account identifiers, so a defect in it can let a caller treat an
invalid IBAN as valid. That makes correctness reports worth handling privately when disclosure
would put consumers at risk.

## Reporting a vulnerability

**Do not open a public issue.** Use GitHub's private vulnerability reporting:

> [Report a vulnerability](https://github.com/BijdorpStudio/kiban/security/advisories/new) —
> the repository's **Security** tab → **Report a vulnerability**

The report is visible only to the maintainers until an advisory is published. Please include what
you can of:

* the input that triggers it (an IBAN string, a country code, whatever the entry point takes),
* which API you called and what it returned or threw, versus what you expected,
* the kiban version, and the target you saw it on (JVM, Android, one of the Native targets, JS or
  Wasm) — several of the paths that matter differ per platform.

You will get an acknowledgement, and the fix and the advisory will credit you unless you ask
otherwise.

## Supported versions

kiban is pre-1.0: only the latest released version is supported, and a fix ships in a new release
rather than as a patch to an older line. Once 1.0 is out this section will say what the supported
line is; [VERSIONING.md](VERSIONING.md) is the policy that will define it.

## What is in scope

* **Validation that is wrong in the permissive direction** — input `Iban(...)`, `toIban()`,
  `toIbanOrNull()` or `isValidIban()` accepts that ISO 13616 and the registry say is not a valid
  IBAN. Accepting an invalid identifier is the failure mode with real consequences downstream.
* **`Modulo97` checksum defects** that make a bad checksum verify.
* **Denial of service from parsing** — an input of bounded length that makes the parser hang,
  recurse without bound or allocate disproportionately.
* **Anything that lets library input reach outside the library**, though the surface for it is
  small: kiban performs no I/O, no network access, no reflection and no deserialization, and
  depends on nothing but the Kotlin standard library.

## What is not

These are ordinary [issues](https://github.com/BijdorpStudio/kiban/issues), not vulnerabilities:

* **Registry data being out of date** — a country added or changed by a SWIFT registry revision the
  released data predates. The data is regenerated from the published registry (see
  [CONTRIBUTING.md](CONTRIBUTING.md)) and lands in a normal release.
* **Rejecting something valid**, and any other false negative — a bug, but it fails closed.
* **National check digits and format masks not being enforced.** kiban deliberately checks length
  and the modulo-97 checksum only; the README's "Design choices" section explains why. Input that a
  country's own national scheme would reject is therefore expected to parse.
* **Vulnerabilities in build-time tooling** (Gradle plugins, the CI workflows, the registry scripts).
  Report those as issues unless they affect a published artifact — nothing under `scripts/`,
  `samples/` or `.github/` is shipped to consumers.
