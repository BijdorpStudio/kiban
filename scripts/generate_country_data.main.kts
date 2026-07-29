#!/usr/bin/env kotlin
/*
 * Generates CountryCodesData.kt and CountryTestData.kt from the SWIFT IBAN Registry TXT.
 *
 * The SWIFT registry TXT ("IBAN Registry (TXT)" on
 * https://www.swift.com/standards/data-standards/iban) must be downloaded manually in a
 * browser - the endpoint blocks non-browser HTTP clients - and must NOT be committed to
 * this repository (it is not redistributable).
 *
 * Usage:
 *     kotlin scripts/generate_country_data.main.kts --registry ~/Downloads/iban-registry-v102.txt --rev 102
 *
 * The registry file is transposed: rows are fields, countries are columns. On top of it the
 * script applies a curated overlay (embedded below): countries from the IBAN.com Experimental
 * List and EPC SEPA participation that the SWIFT registry lags behind on.
 *
 * Every entry is validated before anything is written:
 *  - the example IBAN passes the mod-97 check, has the declared length and country prefix;
 *  - bank/branch identifiers extracted via the declared positions must equal the registry's
 *    own independent "Bank identifier example" / "Branch identifier example" fields, so the
 *    position encoding is cross-checked against data it was not derived from.
 */

@file:DependsOn("com.jsoizo:kotlin-csv-jvm:1.10.0")

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour
import java.io.File
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Curated overlay.
// Sources: IBAN.com Experimental List (https://www.iban.com/structure) via java-iban
// (Apache-2.0, https://github.com/barend/java-iban), and EPC SEPA scheme participation
// (https://www.europeanpaymentscouncil.eu/document-library/other/epc-list-sepa-scheme-countries).
// ---------------------------------------------------------------------------

/** Countries participating in SEPA per EPC that the SWIFT registry does not (yet) flag as SEPA. */
val sepaOverrides = setOf("AL", "MD", "ME", "MK", "RS")

data class ExperimentalCountry(val code: String, val name: String, val example: String)

/** Countries from the IBAN.com Experimental List, absent from the SWIFT registry. */
val experimentalCountries = listOf(
    ExperimentalCountry("AO", "Angola", "AO06004400006729503010102"),
    ExperimentalCountry("BF", "Burkina Faso", "BF42BF0840101300463574000390"),
    ExperimentalCountry("BJ", "Benin", "BJ66BJ0610100100144390000769"),
    ExperimentalCountry("CF", "Central African Republic", "CF4220001000010120069700160"),
    ExperimentalCountry("CG", "Congo", "CG3930011000101013451300019"),
    ExperimentalCountry("CI", "Ivory Coast", "CI93CI0080111301134291200589"),
    ExperimentalCountry("CM", "Cameroon", "CM2110002000300277976315008"),
    ExperimentalCountry("CV", "Cape Verde", "CV64000500000020108215144"),
    ExperimentalCountry("DZ", "Algeria", "DZ580002100001113000000570"),
    ExperimentalCountry("GA", "Gabon", "GA2140021010032001890020126"),
    ExperimentalCountry("GQ", "Equatorial Guinea", "GQ7050002001003715228190196"),
    ExperimentalCountry("GW", "Guinea-Bissau", "GW04GW1430010181800637601"),
    ExperimentalCountry("IR", "Iran", "IR710570029971601460641001"),
    ExperimentalCountry("KM", "Comoros", "KM4600005000010010904400137"),
    ExperimentalCountry("MA", "Morocco", "MA64011519000001205000534921"),
    ExperimentalCountry("MG", "Madagascar", "MG4600005030071289421016045"),
    ExperimentalCountry("ML", "Mali", "ML13ML0160120102600100668497"),
    ExperimentalCountry("MZ", "Mozambique", "MZ59000301080016367102371"),
    ExperimentalCountry("NE", "Niger", "NE58NE0380100100130305000268"),
    ExperimentalCountry("SN", "Senegal", "SN08SN0100152000048500003035"),
    ExperimentalCountry("TD", "Chad", "TD8960002000010271091600153"),
    ExperimentalCountry("TG", "Togo", "TG53TG0090604310346500400070"),
)

// ---------------------------------------------------------------------------
// Model and argument handling
// ---------------------------------------------------------------------------

data class Country(
    val code: String,
    val name: String,
    val swift: Boolean,
    val sepa: Boolean,
    val length: Int,
    /** String.substring indices within the full IBAN, or 0..0 when absent. */
    val bankBegin: Int,
    val bankEnd: Int,
    val branchBegin: Int,
    val branchEnd: Int,
    val example: String,
    /** The registry's own identifier examples, used for cross-validation. Empty for overlay entries. */
    val bankExample: String = "",
    val branchExample: String = "",
)

fun argValue(name: String): String? {
    val index = args.indexOf(name)
    return if (index >= 0 && index + 1 < args.size) args[index + 1] else null
}

val registryPath = argValue("--registry") ?: error("Missing --registry <path to SWIFT registry TXT>")
val rev = argValue("--rev") ?: error("Missing --rev <SWIFT registry revision, e.g. 102>")
val date = argValue("--date") ?: LocalDate.now().toString()

val repoRoot = __FILE__.absoluteFile.parentFile.parentFile
val dataFile = repoRoot.resolve("library/src/commonMain/kotlin/nl/bijdorpstudio/kiban/CountryCodesData.kt")
val testFile = repoRoot.resolve("library/src/commonTest/kotlin/nl/bijdorpstudio/kiban/CountryTestData.kt")

// ---------------------------------------------------------------------------
// Registry TXT parsing (tab-separated; quoted cells may contain newlines)
// ---------------------------------------------------------------------------

fun parseTsv(file: File): List<List<String>> =
    csvReader {
        delimiter = '\t'
        insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.EMPTY_STRING
    }.readAll(file)

/** Parses a "1-4" position within the BBAN into String.substring indices within the IBAN. */
fun parsePosition(cell: String): Pair<Int, Int> {
    val match = Regex("""^(\d+)-(\d+)$""").find(cell.trim()) ?: return 0 to 0
    val (start, end) = match.destructured
    return (4 + start.toInt() - 1) to (4 + end.toInt())
}

fun parseRegistry(file: File): List<Country> {
    val rows = parseTsv(file)
        .filter { it.isNotEmpty() }
        .associate { it.first().trim() to it.drop(1).map(String::trim) }

    val codes = rows.getValue("IBAN prefix country code (ISO 3166)")
    val count = codes.count { it.isNotEmpty() }
    fun field(label: String): List<String> {
        val values = rows[label] ?: emptyList()
        return values + List(maxOf(0, count - values.size)) { "" }
    }

    val names = field("Name of country")
    val sepa = field("SEPA country")
    val bankPositions = field("Bank identifier position within the BBAN")
    val branchPositions = field("Branch identifier position within the BBAN")
    val bankExamples = field("Bank identifier example")
    val branchExamples = field("Branch identifier example")
    val lengths = field("IBAN length")
    val examples = field("IBAN electronic format example")

    return (0 until count).map { i ->
        val (bankBegin, bankEnd) = parsePosition(bankPositions[i])
        val (branchBegin, branchEnd) = parsePosition(branchPositions[i])
        Country(
            code = codes[i],
            name = names[i],
            swift = true,
            sepa = sepa[i] == "Yes",
            length = lengths[i].toInt(),
            bankBegin = bankBegin,
            bankEnd = bankEnd,
            branchBegin = branchBegin,
            branchEnd = branchEnd,
            example = examples[i].replace(" ", ""),
            bankExample = bankExamples[i].replace(" ", ""),
            branchExample = branchExamples[i].replace(" ", ""),
        )
    }
}

// ---------------------------------------------------------------------------
// Merge and validation
// ---------------------------------------------------------------------------

fun merge(registry: List<Country>): List<Country> {
    val registryCodes = registry.map(Country::code).toSet()
    experimentalCountries.firstOrNull { it.code in registryCodes }?.let {
        error("Overlay country ${it.code} is now in the SWIFT registry; remove it from the overlay in this script.")
    }
    val overlaid = registry.map { if (it.code in sepaOverrides) it.copy(sepa = true) else it }
    val experimental = experimentalCountries.map {
        Country(
            code = it.code,
            name = it.name,
            swift = false,
            sepa = false,
            length = it.example.length,
            bankBegin = 0, bankEnd = 0, branchBegin = 0, branchEnd = 0,
            example = it.example,
        )
    }
    return (overlaid + experimental).sortedBy(Country::code)
}

/**
 * Countries whose registry "Bank/Branch identifier example" fields describe a different sample
 * account than the "IBAN electronic format example" (the registry file is internally
 * inconsistent for these). The positional cross-check is skipped for them; if a future registry
 * revision fixes the inconsistency, validation fails so the exception gets removed.
 */
val knownIdentifierExampleMismatches = setOf("BA", "SE")

fun mod97(iban: String): Int {
    val digits = (iban.substring(4) + iban.substring(0, 4))
        .map { it.digitToInt(36).toString() }
        .joinToString("")
    return digits.chunked(9).fold(0L) { acc, chunk -> (acc.toString() + chunk).toLong() % 97 }.toInt()
}

fun validate(countries: List<Country>) {
    val problems = mutableListOf<String>()
    for (c in countries) {
        if (!c.example.startsWith(c.code)) problems += "${c.code}: example ${c.example} has wrong prefix"
        if (c.example.length != c.length) problems += "${c.code}: example length ${c.example.length} != declared ${c.length}"
        if (mod97(c.example) != 1) problems += "${c.code}: example ${c.example} fails mod-97 check"
        if (c.bankBegin > c.bankEnd || c.bankEnd > c.length || c.branchBegin > c.branchEnd || c.branchEnd > c.length) {
            problems += "${c.code}: identifier positions out of range"
        }
        // Cross-check: identifiers cut by position from the example IBAN must equal the
        // registry's independently stated identifier examples.
        val mismatches = mutableListOf<String>()
        if (c.bankBegin > 0 && c.bankExample.isNotEmpty()) {
            val cut = c.example.substring(c.bankBegin, c.bankEnd)
            if (cut != c.bankExample) mismatches += "${c.code}: bank id by position '$cut' != registry example '${c.bankExample}'"
        }
        if (c.branchBegin > 0 && c.branchExample.isNotEmpty() && c.branchExample != "N/A") {
            val cut = c.example.substring(c.branchBegin, c.branchEnd)
            if (cut != c.branchExample) mismatches += "${c.code}: branch id by position '$cut' != registry example '${c.branchExample}'"
        }
        when {
            c.code in knownIdentifierExampleMismatches && mismatches.isEmpty() && c.swift ->
                problems += "${c.code}: listed in knownIdentifierExampleMismatches but now consistent; remove the exception"
            c.code !in knownIdentifierExampleMismatches -> problems += mismatches
        }
    }
    if (problems.isNotEmpty()) {
        error("Registry data failed validation:\n" + problems.joinToString("\n"))
    }
}

// ---------------------------------------------------------------------------
// Code generation
// ---------------------------------------------------------------------------

fun licenseHeader(year: String) = """
    /*
       Copyright $year Barend Garvelink, Eugen Martynov

       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at

           http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.
     */
    package nl.bijdorpstudio.kiban
""".trimIndent()

val regenerateNote =
    "Regenerate with: kotlin scripts/generate_country_data.main.kts --registry <registry.txt> --rev <NN>"

fun generateDataKt(countries: List<Country>): String {
    val codes = countries.joinToString("\n") { "        \"${it.code}\"," }
    val lengths = countries.joinToString("\n") { c ->
        val flags = (if (c.swift) " or SWIFT" else "") + (if (c.sepa) " or SEPA" else "")
        "        /* ${c.code} */ ${c.length}$flags,"
    }
    val bankBranch = countries.joinToString("\n") { c ->
        """
        |        /* ${c.code} */ ${c.bankBegin}
        |                or ((${c.bankBegin} + ${c.bankEnd - c.bankBegin}) shl BANK_IDENTIFIER_END_SHIFT)
        |                or (${c.branchBegin} shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
        |                or ((${c.branchBegin} + ${c.branchEnd - c.branchBegin}) shl BRANCH_IDENTIFIER_END_SHIFT),
        """.trimMargin()
    }
    return """
${licenseHeader(date.substring(0, 4))}

/**
 * Contains information about IBAN country codes. This is a generated file, do not edit manually.
 * $regenerateNote
 * Updated to SWIFT IBAN Registry version $rev on $date.
 */
internal object CountryCodesData {
    /**
     * The "yyyy-MM-dd" datestamp that the embedded IBAN data was updated.
     */
    const val LAST_UPDATE_DATE = "$date"

    /**
     * The revision of the SWIFT IBAN Registry to which the embedded IBAN data was updated.
     */
    const val LAST_UPDATE_REV = "$rev"

    const val SEPA = 1 shl 8
    const val SWIFT = 1 shl 9
    const val REMOVE_METADATA_MASK = 0xFF
    const val BANK_IDENTIFIER_BEGIN_MASK = 0xFF
    const val BANK_IDENTIFIER_END_SHIFT = 8
    const val BANK_IDENTIFIER_END_MASK = 0xFF shl BANK_IDENTIFIER_END_SHIFT
    const val BRANCH_IDENTIFIER_BEGIN_SHIFT = 16
    const val BRANCH_IDENTIFIER_BEGIN_MASK = 0xFF shl BRANCH_IDENTIFIER_BEGIN_SHIFT
    const val BRANCH_IDENTIFIER_END_SHIFT = 24
    const val BRANCH_IDENTIFIER_END_MASK = 0xFF shl BRANCH_IDENTIFIER_END_SHIFT

    /**
     * Known country codes, this list must be sorted to allow binary search. All other lists in this file must use the
     * same indices for the same countries.
     */
    val COUNTRY_CODES: Array<String> = arrayOf(
$codes
    )

    /**
     * Lengths for each country's IBAN. The indices match the indices of [.COUNTRY_CODES], the values are the
     * expected length. Values may embed the [.SEPA] and [.SWIFT] flags to indicate the SEPA membership and
     * whether the record is listed in the SWIFT IBAN Registry.
     */
    val COUNTRY_IBAN_LENGTHS: IntArray = intArrayOf(
$lengths
    )

    /**
     * Contains the start- and end-index (as per [String.substring]) of the bank code and branch code
     * within a country's IBAN format. Mask:
     * <pre>
     * 0x000000FF <- begin offset bank id
     * 0x0000FF00 <- end offset bank id
     * 0x00FF0000 <- begin offset branch id
     * 0xFF000000 <- end offset branch id
    </pre> *
     */
    val BANK_CODE_BRANCH_CODE: IntArray = intArrayOf(
$bankBranch
    )
}
""".trimStart()
}

fun pretty(plain: String): String = plain.chunked(4).joinToString(" ")

fun generateTestKt(countries: List<Country>): String {
    val entries = countries
        .sortedWith(compareBy({ !it.swift }, { if (it.swift) it.code else it.name }))
        .joinToString("\n") { c ->
            val bank = if (c.bankBegin > 0) "\"${c.example.substring(c.bankBegin, c.bankEnd)}\"" else "null"
            val branch = if (c.branchBegin > 0) "\"${c.example.substring(c.branchBegin, c.branchEnd)}\"" else "null"
            """
            |    IbanCountryTestData(
            |        name = "${c.name.replace("\"", "\\\"")}",
            |        swift = ${c.swift},
            |        sepa = ${c.sepa},
            |        plain = "${c.example}",
            |        bank = $bank,
            |        branch = $branch,
            |        pretty = "${pretty(c.example)}"
            |    ),
            """.trimMargin()
        }
    return """
${licenseHeader(date.substring(0, 4))}

/**
 * Valid example IBANs for every known country. This is a generated file, do not edit manually.
 * $regenerateNote
 * Updated to SWIFT IBAN Registry version $rev on $date.
 *
 * Bank and branch identifier expectations are cross-validated at generation time against the
 * registry's own "Bank identifier example" and "Branch identifier example" fields, which are
 * independent of the position data embedded in CountryCodesData.
 *
 * References:
 * - SWIFT IBAN Registry: https://www.swift.com/standards/data-standards/iban
 * - IBAN.com Experimental List: https://www.iban.com/structure
 */
internal val countryTestData = listOf(
$entries
)
""".trimStart()
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

val countries = merge(parseRegistry(File(registryPath)))
validate(countries)
dataFile.writeText(generateDataKt(countries))
testFile.writeText(generateTestKt(countries))
val swiftCount = countries.count(Country::swift)
println("Generated data for ${countries.size} countries ($swiftCount SWIFT, ${countries.size - swiftCount} experimental) at registry rev $rev.")
println("Wrote ${dataFile.relativeTo(repoRoot)} and ${testFile.relativeTo(repoRoot)}.")
