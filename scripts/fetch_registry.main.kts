#!/usr/bin/env kotlin
/*
 * Downloads the SWIFT IBAN Registry TXT through a real browser engine.
 *
 * The download endpoint blocks plain HTTP clients below the HTTP layer: the TLS handshake is
 * dropped on fingerprint, so no shell script or HTTP library ever sees a status code. A real
 * Chromium context that has actually loaded the registry page can fetch it: a programmatic
 * fetch() from page context returns the TXT with HTTP 200.
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
 * --rev overrides the revision this script detects from the download's filename or the page
 * text; --self-check runs the offline assertions over the parsing helpers and exits.
 *
 * Bot detection is an arms race this project does not control, so treat the automated path as
 * best-effort convenience: downloading the TXT manually in a browser into scripts/input/ and
 * running the generator by hand stays the guaranteed fallback, and nothing is lost but
 * convenience if this stops working.
 *
 * Inside GitHub Actions (when $GITHUB_ENV is set) the script exports REGISTRY_TXT,
 * REGISTRY_SHA256 and REGISTRY_REV for the steps that follow.
 */

@file:DependsOn("com.microsoft.playwright:playwright:1.62.0")

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import java.io.File
import java.security.MessageDigest

val registryPage = "https://www.swift.com/standards/data-standards/iban"
val registryTxt = "https://www.swift.com/swift-resource/11971/download"

/** First column of the registry TXT's first data row; absent from any bot-check or error page. */
val registryMarker = "IBAN prefix country code (ISO 3166)"

/** The registry TXT is ~34 KB; anything much smaller is not the registry. */
val minimumRegistrySize = 10_000

// ---------------------------------------------------------------------------
// Parsing helpers (pure; covered by --self-check)
// ---------------------------------------------------------------------------

/** "IBAN Registry Release 99", "iban-registry-v100.txt" - the revision next to the registry name. */
val registryReleaseRegex =
    Regex(
        """iban[\s_-]*registry[\s_-]*(?:\(txt\)[\s_-]*)?(?:release|version|revision|rev\.?|v)?[\s_-]*(\d{2,3})(?!\d)""",
        RegexOption.IGNORE_CASE,
    )

/** "Release 99", "v100" - only trusted in a filename, where there is nothing else to match. */
val releaseNumberRegex =
    Regex("""(?:release|version|revision|rev\.?|v)[\s_-]*(\d{2,3})(?!\d)""", RegexOption.IGNORE_CASE)

/** Pulls the filename out of a Content-Disposition header, quoted, bare or RFC 5987 encoded. */
fun filenameIn(contentDisposition: String): String? =
    Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""", RegexOption.IGNORE_CASE)
        .find(contentDisposition)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/** The registry revision as encoded in the download's filename, or null if it carries none. */
fun revisionInFilename(filename: String): String? =
    (registryReleaseRegex.find(filename) ?: releaseNumberRegex.find(filename))?.groupValues?.get(1)

/**
 * The registry revision as stated on the registry page. Only the revision written next to the
 * registry's own name counts - a bare "version 3" somewhere else on the page is not it.
 */
fun revisionInPageText(pageText: String): String? =
    registryReleaseRegex.find(pageText)?.groupValues?.get(1)

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
            text: text,
        }
    }
    """
        .trimIndent()

data class RegistryDownload(
    val status: Int,
    val contentType: String,
    val disposition: String,
    val text: String,
    val pageText: String,
)

fun downloadRegistry(): RegistryDownload =
    Playwright.create().use { playwright ->
        playwright
            .chromium()
            .launch(BrowserType.LaunchOptions().setHeadless(!headed))
            .use { browser ->
                val context =
                    browser.newContext(
                        Browser.NewContextOptions().setLocale("en-US").setViewportSize(1280, 900)
                    )
                context.setDefaultTimeout(timeoutMillis)
                val page = context.newPage()
                page.navigate(registryPage, Page.NavigateOptions().setTimeout(timeoutMillis))
                page.waitForLoadState(LoadState.DOMCONTENTLOADED)

                @Suppress("UNCHECKED_CAST")
                val response = page.evaluate(fetchInPageContext, registryTxt) as Map<String, Any?>
                RegistryDownload(
                    status = (response["status"] as Number).toInt(),
                    contentType = response["contentType"] as String,
                    disposition = response["disposition"] as String,
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
                "\n\nBot detection may have caught this run. Retry with --headed (under xvfb on a " +
                "headless host), or download the TXT manually in a browser from $registryPage " +
                "and pass it to scripts/generate_country_data.main.kts."
        )
    }

    val filename = filenameIn(download.disposition)
    val rev =
        revOverride
            ?: filename?.let(::revisionInFilename)
            ?: revisionInPageText(download.pageText)

    val out = File(outPath).absoluteFile
    out.parentFile?.mkdirs()
    out.writeText(download.text)
    val digest = sha256(download.text)

    println("Downloaded ${download.text.length} characters to ${out.path}")
    println("sha256: $digest")
    if (rev != null) {
        println("Registry revision: $rev${if (revOverride != null) " (from --rev)" else ""}")
    } else {
        println(
            "Could not detect the registry revision from the download filename " +
                "(${filename ?: "none"}) or the page text; pass --rev <NN> explicitly."
        )
    }

    System.getenv("GITHUB_ENV")?.let { githubEnv ->
        File(githubEnv).appendText(
            buildString {
                appendLine("REGISTRY_TXT=${out.path}")
                appendLine("REGISTRY_SHA256=$digest")
                if (rev != null) appendLine("REGISTRY_REV=$rev")
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Self-check: offline assertions over the parsing helpers
// ---------------------------------------------------------------------------

fun selfCheck() {
    var checks = 0
    val failures = mutableListOf<String>()
    fun expect(what: String, expected: Any?, actual: Any?) {
        checks++
        if (expected != actual) failures += "$what: expected <$expected>, got <$actual>"
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

    expect("revision in an underscored filename", "99", revisionInFilename("IBAN_Registry_Release_99.txt"))
    expect("revision in a spaced filename", "102", revisionInFilename("IBAN Registry Release 102.txt"))
    expect("revision in a dashed filename", "100", revisionInFilename("iban-registry-v100.txt"))
    expect("revision in a bare release filename", "97", revisionInFilename("registry-release-97.txt"))
    expect("revision in an unnumbered filename", null, revisionInFilename("swift_registry.txt"))

    expect(
        "revision in page text",
        "99",
        revisionInPageText("Download the IBAN Registry (TXT) Release 99 below."),
    )
    expect(
        "revision in page text without a release word",
        "102",
        revisionInPageText("IBAN REGISTRY 102 - published May 2026"),
    )
    expect("revision in page text without a number", null, revisionInPageText("IBAN Registry (TXT)"))
    expect(
        "revision in page text with an unrelated number",
        null,
        revisionInPageText("Version 3 of our cookie policy applies to the IBAN Registry page."),
    )
    expect(
        "revision in page text with a year after the name",
        null,
        revisionInPageText("IBAN Registry 2026 edition"),
    )

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
        "sha256 of the standard test vector",
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        sha256("abc"),
    )

    if (failures.isNotEmpty()) {
        error("$checks self-checks ran, ${failures.size} failed:\n" + failures.joinToString("\n") { " - $it" })
    }
    println("All $checks self-checks passed.")
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

if (args.contains("--self-check")) selfCheck() else fetchRegistry()
