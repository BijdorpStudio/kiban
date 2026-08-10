#!/usr/bin/env kotlin
/*
 * Downloads the SWIFT IBAN Registry TXT through a real browser engine.
 *
 * Swift blocks unrecognised clients below the HTTP layer, so no shell script or HTTP library ever
 * sees a status code - the connection just hangs and is eventually reset. This covers the
 * registry page itself, not only the download endpoint, which is why failures surface as
 * navigation timeouts rather than as a bot-check page. A real Chromium context that has actually
 * loaded the page can fetch it: a programmatic fetch() from page context returns the TXT with
 * HTTP 200. Headless Chromium is itself blocked from some networks even where headed works, so
 * --headed is a genuine fallback and not just a debugging aid.
 *
 * The downloaded TXT is NOT redistributable and must never be committed. The default output
 * path lives under scripts/input/, which is gitignored; only the artifacts generated from it
 * (CountryCodesData.kt, CountryTestData.kt) belong in the repository.
 *
 * Usage:
 *     kotlin scripts/fetch_registry.main.kts [--out <path>] [--rev NN] [--headed] [--timeout 60]
 *     kotlin scripts/fetch_registry.main.kts --self-check
 *
 * and then feed the result to the generator:
 *     kotlin scripts/generate_country_data.main.kts --registry scripts/input/iban-registry.txt --rev <NN>
 *
 * --rev records the registry revision; --self-check runs the offline assertions over the parsing
 * helpers and exits. The revision cannot currently be detected: the download endpoint sends no
 * Content-Disposition header at all, and the registry page names no release number anywhere. The
 * detection below is therefore best-effort against a page that may start stating one again, and
 * an unknown revision is the normal outcome rather than a failure - it only has to be resolved
 * once the registry TXT has actually changed, which is why registry-sync.yml regenerates and
 * diffs first and demands a revision second.
 *
 * Bot detection is an arms race this project does not control, so treat the automated path as
 * best-effort convenience: downloading the TXT manually in a browser into scripts/input/ and
 * running the generator by hand stays the guaranteed fallback, and nothing is lost but
 * convenience if this stops working.
 *
 * Inside GitHub Actions (when $GITHUB_ENV is set) the script exports REGISTRY_TXT,
 * REGISTRY_SHA256, REGISTRY_LAST_MODIFIED and - when known - REGISTRY_REV for the steps that
 * follow.
 */

@file:DependsOn("com.microsoft.playwright:playwright:1.62.0")

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import java.io.File
import java.security.MessageDigest

val registryPage = "https://www.swift.com/standards/data-standards/iban"
val registryTxt = "https://www.swift.com/swift-resource/11971/download"

/** First column of the registry TXT's first data row; absent from any bot-check or error page. */
val registryMarker = "IBAN prefix country code (ISO 3166)"

/** The registry TXT is ~34 KB; anything much smaller is not the registry. */
val minimumRegistrySize = 10_000

/** How to get the registry by hand; every acquisition failure ends with this. */
val fallbackAdvice =
    "Bot detection may have caught this run. Retry with --headed (under xvfb on a headless " +
        "host), or download the TXT manually in a browser from $registryPage and pass it to " +
        "scripts/generate_country_data.main.kts."

// ---------------------------------------------------------------------------
// Parsing helpers (pure; covered by --self-check)
// ---------------------------------------------------------------------------

/**
 * "IBAN Registry Release 99", "iban-registry-v100.txt": a revision only counts when it is
 * written next to the registry's own name, so a stray "version 3" elsewhere on the page is not
 * mistaken for one.
 */
val registryReleaseRegex =
    Regex(
        """iban[\s_-]*registry[\s_-]*(?:\(txt\)[\s_-]*)?(?:release|version|revision|rev\.?|v)?[\s_-]*(\d{2,3})(?!\d)""",
        RegexOption.IGNORE_CASE,
    )

/** Pulls the filename out of a Content-Disposition header, quoted, bare or RFC 5987 encoded. */
fun filenameIn(contentDisposition: String): String? =
    Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""", RegexOption.IGNORE_CASE)
        .find(contentDisposition)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/** The registry revision stated in [text], or null when it states none. */
fun revisionIn(text: String): String? = registryReleaseRegex.find(text)?.groupValues?.get(1)

/** Everything that disqualifies a response from being the registry TXT. */
fun problemsWith(status: Int, contentType: String, text: String): List<String> = buildList {
    if (status != 200) add("expected HTTP 200, got HTTP $status")
    if (contentType.isNotEmpty() && !contentType.startsWith("text/")) {
        add("expected a text response, got content type '$contentType'")
    }
    if (text.length < minimumRegistrySize) {
        add("response is only ${text.length} characters, expected at least $minimumRegistrySize")
    }
    if (registryMarker !in text) {
        add("response does not contain the registry row '$registryMarker' - most likely a bot-check page")
    }
}

/**
 * Condenses a Playwright failure into the one line that says what went wrong: it reports errors
 * as a multi-line `Error { ... }` dump whose first line carries no information.
 */
fun summarize(message: String?): String {
    val lines = message?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    val stated = lines.firstOrNull { it.startsWith("message='") }?.removePrefix("message='")?.trim()
    return stated?.takeIf { it.isNotEmpty() } ?: lines.firstOrNull() ?: "no details"
}

/**
 * The `KEY=value` block the workflow steps read back out of `$GITHUB_ENV`. Unknown values are
 * omitted rather than exported empty, so the workflow can test for their presence.
 */
fun githubEnvExports(path: String, digest: String, lastModified: String?, rev: String?): String =
    buildString {
        appendLine("REGISTRY_TXT=$path")
        appendLine("REGISTRY_SHA256=$digest")
        if (!lastModified.isNullOrEmpty()) appendLine("REGISTRY_LAST_MODIFIED=$lastModified")
        if (rev != null) appendLine("REGISTRY_REV=$rev")
    }

/** Runs [step], turning a failure into an error that says what broke and how to work around it. */
fun <T> attempting(what: String, step: () -> T): T =
    runCatching(step).getOrElse {
        error("Could not $what: ${summarize(it.message)}\n\n$fallbackAdvice")
    }

fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") {
        "%02x".format(it)
    }

// ---------------------------------------------------------------------------
// Argument handling
// ---------------------------------------------------------------------------

fun argValue(name: String): String? {
    val index = args.indexOf(name)
    return if (index >= 0 && index + 1 < args.size) args[index + 1] else null
}

val repoRoot = __FILE__.absoluteFile.parentFile.parentFile
val outPath = argValue("--out") ?: repoRoot.resolve("scripts/input/iban-registry.txt").path
val revOverride = argValue("--rev")
val headed = args.contains("--headed")
val timeoutMillis = (argValue("--timeout")?.toDoubleOrNull() ?: 60.0) * 1000

// ---------------------------------------------------------------------------
// Download
// ---------------------------------------------------------------------------

/**
 * Fetches [url] from within the loaded page, so the request carries the page's origin,
 * cookies and TLS fingerprint. Returns the fields of the response the caller validates on.
 */
val fetchInPageContext =
    """
    async (url) => {
        const response = await fetch(url, { credentials: 'include' })
        const text = await response.text()
        return {
            status: response.status,
            contentType: response.headers.get('content-type') || '',
            disposition: response.headers.get('content-disposition') || '',
            lastModified: response.headers.get('last-modified') || '',
            text: text,
        }
    }
    """
        .trimIndent()

data class RegistryDownload(
    val status: Int,
    val contentType: String,
    val disposition: String,
    val lastModified: String,
    val text: String,
    val pageText: String,
)

fun downloadRegistry(): RegistryDownload =
    Playwright.create().use { playwright ->
        playwright
            .chromium()
            .launch(BrowserType.LaunchOptions().setHeadless(!headed))
            .use { browser ->
                val page = browser.newPage()
                page.setDefaultTimeout(timeoutMillis)
                // Blocked clients are dropped below the HTTP layer, so this hangs until it times
                // out rather than returning a page that problemsWith() could report on.
                attempting("load $registryPage") { page.navigate(registryPage) }

                val response =
                    attempting("fetch $registryTxt from the loaded page") {
                        @Suppress("UNCHECKED_CAST")
                        page.evaluate(fetchInPageContext, registryTxt) as Map<String, Any?>
                    }
                RegistryDownload(
                    status = (response["status"] as Number).toInt(),
                    contentType = response["contentType"] as String,
                    disposition = response["disposition"] as String,
                    lastModified = response["lastModified"] as String,
                    text = response["text"] as String,
                    pageText = runCatching { page.innerText("body") }.getOrDefault(""),
                )
            }
    }

fun fetchRegistry() {
    println("Loading $registryPage in ${if (headed) "headed" else "headless"} Chromium...")
    val download = downloadRegistry()

    val problems = problemsWith(download.status, download.contentType, download.text)
    if (problems.isNotEmpty()) {
        error(
            "The registry download did not return the registry TXT:\n" +
                problems.joinToString("\n") { " - $it" } +
                "\n\n" +
                fallbackAdvice
        )
    }

    val filename = filenameIn(download.disposition)
    val rev = revOverride ?: filename?.let(::revisionIn) ?: revisionIn(download.pageText)

    val out = File(outPath).absoluteFile
    out.parentFile?.mkdirs()
    out.writeText(download.text)
    val digest = sha256(download.text)

    println("Downloaded ${download.text.length} characters to ${out.path}")
    println("sha256: $digest")
    if (download.lastModified.isNotEmpty()) println("last-modified: ${download.lastModified}")
    if (rev != null) {
        println("Registry revision: $rev${if (revOverride != null) " (from --rev)" else ""}")
    } else {
        println(
            "Registry revision: unknown, which is the norm - the download states none in a " +
                "Content-Disposition filename (${filename ?: "no filename"}) and the page names " +
                "no release. Pass --rev <NN> to record one; it is only needed once the generated " +
                "data actually changes."
        )
    }

    System.getenv("GITHUB_ENV")?.let { githubEnv ->
        File(githubEnv).appendText(
            githubEnvExports(out.path, digest, download.lastModified, rev)
        )
    }
}

// ---------------------------------------------------------------------------
// Self-check: offline assertions over the parsing helpers
// ---------------------------------------------------------------------------

fun selfCheck() {
    var checks = 0
    fun expect(what: String, expected: Any?, actual: Any?) {
        check(expected == actual) { "$what: expected <$expected>, got <$actual>" }
        checks++
    }

    expect(
        "filename in a quoted disposition",
        "IBAN_Registry_Release_99.txt",
        filenameIn("""attachment; filename="IBAN_Registry_Release_99.txt""""),
    )
    expect(
        "filename in a bare disposition",
        "IBAN Registry Release 102.txt",
        filenameIn("attachment; filename=IBAN Registry Release 102.txt"),
    )
    expect(
        "filename in an RFC 5987 disposition",
        "IBAN_Registry_Release_100.txt",
        filenameIn("attachment; filename*=UTF-8''IBAN_Registry_Release_100.txt"),
    )
    expect("filename in an empty disposition", null, filenameIn(""))
    expect("filename in a disposition without one", null, filenameIn("inline"))

    expect("revision in an underscored filename", "99", revisionIn("IBAN_Registry_Release_99.txt"))
    expect("revision in a spaced filename", "102", revisionIn("IBAN Registry Release 102.txt"))
    expect("revision in a dashed filename", "100", revisionIn("iban-registry-v100.txt"))
    expect("revision in an unnumbered filename", null, revisionIn("swift_registry.txt"))

    expect("revision in page text", "99", revisionIn("Download the IBAN Registry (TXT) Release 99 below."))
    expect("revision in page text without a release word", "102", revisionIn("IBAN REGISTRY 102 - May 2026"))
    expect("revision in page text without a number", null, revisionIn("IBAN Registry (TXT)"))
    expect(
        "revision in page text with an unrelated number",
        null,
        revisionIn("Version 3 of our cookie policy applies to the IBAN Registry page."),
    )
    expect("revision in page text with a year after the name", null, revisionIn("IBAN Registry 2026 edition"))

    val registryLike = registryMarker + "\t" + "AD\tAE\n".repeat(2_000)
    expect("problems with a registry response", emptyList<String>(), problemsWith(200, "text/plain", registryLike))
    expect(
        "problems with a charset-qualified content type",
        emptyList<String>(),
        problemsWith(200, "text/plain; charset=utf-8", registryLike),
    )
    expect("problems with a forbidden response", 1, problemsWith(403, "text/plain", registryLike).size)
    // A bot-check page is too short and lacks the marker row: two distinct problems.
    expect(
        "problems with a bot-check page",
        2,
        problemsWith(200, "text/html", "<html>Access denied</html>").size,
    )
    expect(
        "problems with a binary response",
        3,
        problemsWith(200, "application/octet-stream", "PK").size,
    )
    expect(
        "problems with a truncated registry",
        1,
        problemsWith(200, "text/plain", registryMarker + "\tAD\n").size,
    )

    expect(
        "summary of a Playwright error dump",
        "Timeout 60000ms exceeded.",
        summarize("com.microsoft.playwright.TimeoutError: Error {\n  message='Timeout 60000ms exceeded.\n  name='TimeoutError\n}"),
    )
    expect(
        "summary of a single-line failure",
        "net::ERR_CONNECTION_RESET",
        summarize("net::ERR_CONNECTION_RESET"),
    )
    expect("summary of a failure without a message", "no details", summarize(null))
    expect("summary of a blank message", "no details", summarize("   \n  "))

    expect(
        "GitHub env exports with everything known",
        "REGISTRY_TXT=/w/registry.txt\n" +
            "REGISTRY_SHA256=abc123\n" +
            "REGISTRY_LAST_MODIFIED=Thu, 18 Jun 2026 08:36:58 GMT\n" +
            "REGISTRY_REV=102\n",
        githubEnvExports("/w/registry.txt", "abc123", "Thu, 18 Jun 2026 08:36:58 GMT", "102"),
    )
    // The download endpoint sends no Content-Disposition, so an undetected revision is the norm:
    // REGISTRY_REV must then be absent rather than empty, or the workflow's -z test would pass.
    expect(
        "GitHub env exports without a revision",
        "REGISTRY_TXT=/w/registry.txt\nREGISTRY_SHA256=abc123\n",
        githubEnvExports("/w/registry.txt", "abc123", null, null),
    )

    // A connection reset dies inside page.navigate, before any response exists to validate, so
    // the acquisition steps have to carry the fallback guidance themselves.
    val failed =
        runCatching { attempting("load the registry page") { error("Timeout 60000ms exceeded.") } }
            .exceptionOrNull()
    expect(
        "a failed step says what it was doing",
        "Could not load the registry page: Timeout 60000ms exceeded.",
        failed?.message?.lineSequence()?.first(),
    )
    expect(
        "a failed step ends with the manual fallback",
        true,
        failed?.message?.contains("download the TXT manually in a browser"),
    )
    expect("a successful step returns its value", 42, attempting("count") { 42 })

    expect(
        "sha256 of the standard test vector",
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        sha256("abc"),
    )

    println("All $checks self-checks passed.")
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

if (args.contains("--self-check")) selfCheck() else fetchRegistry()
