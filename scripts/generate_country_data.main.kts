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
 *     kotlin scripts/generate_country_data.main.kts --self-check
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
 *
 * --self-check runs the parser, the overlay merge and the validation over the fixtures in
 * scripts/testdata/ - invented countries in the registry's own format, because the real TXT
 * cannot be committed - and exits without writing anything. It is what turns a format-handling
 * regression into a failing check rather than a bad weekly sync.
 */

@file:DependsOn("com.jsoizo:kotlin-csv-jvm:1.10.0")
@file:DependsOn("com.squareup:kotlinpoet-jvm:2.2.0")

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
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

/**
 * Location of an embedded identifier as String.substring indices within the full IBAN.
 * [ABSENT] when the country does not embed the identifier.
 */
data class IdentifierPosition(val begin: Int, val end: Int) {
    val present: Boolean get() = begin > 0
    val length: Int get() = end - begin

    fun cutFrom(iban: String): String? = if (present) iban.substring(begin, end) else null

    companion object {
        val ABSENT = IdentifierPosition(0, 0)

        /** Parses a "1-4" position within the BBAN, as used by the registry TXT. */
        fun ofBban(cell: String): IdentifierPosition {
            val match = Regex("""^(\d+)-(\d+)$""").find(cell.trim()) ?: return ABSENT
            val (start, end) = match.destructured
            return IdentifierPosition(4 + start.toInt() - 1, 4 + end.toInt())
        }
    }
}

data class Country(
    val code: String,
    val name: String,
    val swift: Boolean,
    val sepa: Boolean,
    val length: Int,
    val bank: IdentifierPosition,
    val branch: IdentifierPosition,
    val example: String,
    /** The registry's own identifier examples, used for cross-validation. Empty for overlay entries. */
    val bankExample: String = "",
    val branchExample: String = "",
)

fun argValue(name: String): String? {
    val index = args.indexOf(name)
    return if (index >= 0 && index + 1 < args.size) args[index + 1] else null
}

/**
 * Metadata the generator only stamps into its output: neither value is derived from the registry
 * file, which states no revision and carries no generation date.
 */
data class Stamp(val rev: String, val date: String)

val repoRoot = __FILE__.absoluteFile.parentFile.parentFile
val dataFile = repoRoot.resolve("library/src/commonMain/kotlin/nl/bijdorpstudio/kiban/CountryCodesData.kt")
val testFile = repoRoot.resolve("library/src/commonTest/kotlin/nl/bijdorpstudio/kiban/CountryTestData.kt")

/** Invented registry entries covering the parser's edge cases; see --self-check. */
val syntheticRegistry = repoRoot.resolve("scripts/testdata/synthetic-registry.txt")

/** A registry stripped to the rows the parser cannot do without; see --self-check. */
val minimalRegistry = repoRoot.resolve("scripts/testdata/synthetic-registry-minimal.txt")

// ---------------------------------------------------------------------------
// Registry TXT parsing (tab-separated; quoted cells may contain newlines)
// ---------------------------------------------------------------------------

fun parseTsv(file: File): List<List<String>> =
    csvReader {
        delimiter = '\t'
        insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.EMPTY_STRING
    }.readAll(file)

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
        Country(
            code = codes[i],
            name = names[i],
            swift = true,
            sepa = sepa[i] == "Yes",
            length = lengths[i].toInt(),
            bank = IdentifierPosition.ofBban(bankPositions[i]),
            branch = IdentifierPosition.ofBban(branchPositions[i]),
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
            bank = IdentifierPosition.ABSENT,
            branch = IdentifierPosition.ABSENT,
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
        val structural = mutableListOf<String>()
        if (!c.example.startsWith(c.code)) structural += "${c.code}: example ${c.example} has wrong prefix"
        if (c.example.length != c.length) structural += "${c.code}: example length ${c.example.length} != declared ${c.length}"
        if (mod97(c.example) != 1) structural += "${c.code}: example ${c.example} fails mod-97 check"
        if (c.bank.begin > c.bank.end || c.bank.end > c.length || c.branch.begin > c.branch.end || c.branch.end > c.length) {
            structural += "${c.code}: identifier positions out of range"
        }
        problems += structural
        // Cross-check: identifiers cut by position from the example IBAN must equal the
        // registry's independently stated identifier examples. Only meaningful for an entry whose
        // example and positions agree on a length: cutting from a garbled one dies in substring()
        // instead of reporting what is wrong with it, and the structural problems already say so.
        if (structural.isNotEmpty()) continue
        val mismatches = mutableListOf<String>()
        if (c.bank.present && c.bankExample.isNotEmpty()) {
            val cut = c.bank.cutFrom(c.example)
            if (cut != c.bankExample) mismatches += "${c.code}: bank id by position '$cut' != registry example '${c.bankExample}'"
        }
        if (c.branch.present && c.branchExample.isNotEmpty() && c.branchExample != "N/A") {
            val cut = c.branch.cutFrom(c.example)
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
// Code generation (KotlinPoet)
// ---------------------------------------------------------------------------

val pkg = "nl.bijdorpstudio.kiban"

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

""".trimIndent()

val regenerateNote =
    "Regenerate with: kotlin scripts/generate_country_data.main.kts --registry <registry.txt> --rev <NN>"

fun generatedKdoc(what: String, stamp: Stamp) =
    "$what This is a generated file, do not edit manually.\n$regenerateNote\n" +
        "Updated to SWIFT IBAN Registry version ${stamp.rev} on ${stamp.date}.\n"

fun constInt(name: String, format: String, vararg args: Any): PropertySpec =
    PropertySpec.builder(name, INT, KModifier.CONST)
        .initializer(format, *args)
        .build()

fun dataFileSpec(countries: List<Country>, stamp: Stamp): FileSpec {
    val sepa = constInt("SEPA", "1 shl 8")
    val swift = constInt("SWIFT", "1 shl 9")
    val bankEndShift = constInt("BANK_IDENTIFIER_END_SHIFT", "8")
    val branchBeginShift = constInt("BRANCH_IDENTIFIER_BEGIN_SHIFT", "16")
    val branchEndShift = constInt("BRANCH_IDENTIFIER_END_SHIFT", "24")

    val codesInitializer = buildCodeBlock {
        add("arrayOf(\n")
        indent()
        add(countries.map { CodeBlock.of("%S", it.code) }.joinToCode(",\n", suffix = ",\n"))
        unindent()
        add(")")
    }
    val lengthsInitializer = buildCodeBlock {
        add("intArrayOf(\n")
        indent()
        add(
            countries.map { c ->
                buildCodeBlock {
                    add("/* %L */ %L", c.code, c.length)
                    if (c.swift) add(" or %N", swift)
                    if (c.sepa) add(" or %N", sepa)
                }
            }.joinToCode(",\n", suffix = ",\n")
        )
        unindent()
        add(")")
    }
    val bankBranchInitializer = buildCodeBlock {
        add("intArrayOf(\n")
        indent()
        add(
            countries.map { c ->
                buildCodeBlock {
                    add("/* %L */ %L\n", c.code, c.bank.begin)
                    indent()
                    indent()
                    add("or ((%L + %L) shl %N)\n", c.bank.begin, c.bank.length, bankEndShift)
                    add("or (%L shl %N)\n", c.branch.begin, branchBeginShift)
                    add("or ((%L + %L) shl %N)", c.branch.begin, c.branch.length, branchEndShift)
                    unindent()
                    unindent()
                }
            }.joinToCode(",\n", suffix = ",\n")
        )
        unindent()
        add(")")
    }

    val dataObject = TypeSpec.objectBuilder("CountryCodesData")
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(generatedKdoc("Contains information about IBAN country codes.", stamp))
        .addProperty(
            PropertySpec.builder("LAST_UPDATE_DATE", STRING, KModifier.CONST)
                .addKdoc("The \"yyyy-MM-dd\" datestamp that the embedded IBAN data was updated.\n")
                .initializer("%S", stamp.date)
                .build()
        )
        .addProperty(
            PropertySpec.builder("LAST_UPDATE_REV", STRING, KModifier.CONST)
                .addKdoc("The revision of the SWIFT IBAN Registry to which the embedded IBAN data was updated.\n")
                .initializer("%S", stamp.rev)
                .build()
        )
        .addProperty(sepa)
        .addProperty(swift)
        .addProperty(constInt("REMOVE_METADATA_MASK", "0xFF"))
        .addProperty(constInt("BANK_IDENTIFIER_BEGIN_MASK", "0xFF"))
        .addProperty(bankEndShift)
        .addProperty(constInt("BANK_IDENTIFIER_END_MASK", "0xFF shl %N", bankEndShift))
        .addProperty(branchBeginShift)
        .addProperty(constInt("BRANCH_IDENTIFIER_BEGIN_MASK", "0xFF shl %N", branchBeginShift))
        .addProperty(branchEndShift)
        .addProperty(constInt("BRANCH_IDENTIFIER_END_MASK", "0xFF shl %N", branchEndShift))
        .addProperty(
            PropertySpec.builder("COUNTRY_CODES", ARRAY.parameterizedBy(STRING))
                .addKdoc(
                    "Known country codes, this list must be sorted to allow binary search. " +
                        "All other lists in this file must use the\nsame indices for the same countries.\n"
                )
                .initializer(codesInitializer)
                .build()
        )
        .addProperty(
            PropertySpec.builder("COUNTRY_IBAN_LENGTHS", INT_ARRAY)
                .addKdoc(
                    "Lengths for each country's IBAN. The indices match the indices of [COUNTRY_CODES], " +
                        "the values are the\nexpected length. Values may embed the [SEPA] and [SWIFT] flags " +
                        "to indicate the SEPA membership and\nwhether the record is listed in the SWIFT IBAN Registry.\n"
                )
                .initializer(lengthsInitializer)
                .build()
        )
        .addProperty(
            PropertySpec.builder("BANK_CODE_BRANCH_CODE", INT_ARRAY)
                .addKdoc(
                    "Contains the start- and end-index (as per [String.substring]) of the bank code " +
                        "and branch code\nwithin a country's IBAN format. Mask:\n```\n" +
                        "0x000000FF <- begin offset bank id\n" +
                        "0x0000FF00 <- end offset bank id\n" +
                        "0x00FF0000 <- begin offset branch id\n" +
                        "0xFF000000 <- end offset branch id\n```\n"
                )
                .initializer(bankBranchInitializer)
                .build()
        )
        .build()

    return FileSpec.builder(pkg, "CountryCodesData")
        .indent("    ")
        .addType(dataObject)
        .build()
}

fun pretty(plain: String): String = plain.chunked(4).joinToString(" ")

fun testFileSpec(countries: List<Country>, stamp: Stamp): FileSpec {
    val testDataType = ClassName(pkg, "IbanCountryTestData")
    val entriesInitializer = buildCodeBlock {
        add("listOf(\n")
        indent()
        add(
            countries
                .sortedWith(compareBy({ !it.swift }, { if (it.swift) it.code else it.name }))
                .map { c ->
                    buildCodeBlock {
                        add("%T(\n", testDataType)
                        indent()
                        add("name = %S,\n", c.name)
                        add("swift = %L,\n", c.swift)
                        add("sepa = %L,\n", c.sepa)
                        add("plain = %S,\n", c.example)
                        add("bank = %L,\n", c.bank.cutFrom(c.example)?.let { CodeBlock.of("%S", it) } ?: "null")
                        add("branch = %L,\n", c.branch.cutFrom(c.example)?.let { CodeBlock.of("%S", it) } ?: "null")
                        add("pretty = %S,\n", pretty(c.example))
                        unindent()
                        add(")")
                    }
                }.joinToCode(",\n", suffix = ",\n")
        )
        unindent()
        add(")")
    }

    val property = PropertySpec.builder("countryTestData", LIST.parameterizedBy(testDataType), KModifier.INTERNAL)
        .addKdoc(
            generatedKdoc("Valid example IBANs for every known country.", stamp) +
                "\nBank and branch identifier expectations are cross-validated at generation time against the\n" +
                "registry's own \"Bank identifier example\" and \"Branch identifier example\" fields, which are\n" +
                "independent of the position data embedded in CountryCodesData.\n" +
                "\nReferences:\n" +
                "- SWIFT IBAN Registry: https://www.swift.com/standards/data-standards/iban\n" +
                "- IBAN.com Experimental List: https://www.iban.com/structure\n"
        )
        .initializer(entriesInitializer)
        .build()

    return FileSpec.builder(pkg, "CountryTestData")
        .indent("    ")
        .addProperty(property)
        .build()
}

// ---------------------------------------------------------------------------
// Generation
// ---------------------------------------------------------------------------

fun generate() {
    val registryPath = argValue("--registry") ?: error("Missing --registry <path to SWIFT registry TXT>")
    val stamp = Stamp(
        rev = argValue("--rev") ?: error("Missing --rev <SWIFT registry revision, e.g. 102>"),
        date = argValue("--date") ?: LocalDate.now().toString(),
    )

    val countries = merge(parseRegistry(File(registryPath)))
    validate(countries)
    dataFile.writeText(licenseHeader(stamp.date.substring(0, 4)) + dataFileSpec(countries, stamp))
    testFile.writeText(licenseHeader(stamp.date.substring(0, 4)) + testFileSpec(countries, stamp))
    val swiftCount = countries.count(Country::swift)
    println("Generated data for ${countries.size} countries ($swiftCount SWIFT, ${countries.size - swiftCount} experimental) at registry rev ${stamp.rev}.")
    println("Wrote ${dataFile.relativeTo(repoRoot)} and ${testFile.relativeTo(repoRoot)}.")
}

// ---------------------------------------------------------------------------
// Self-check: offline assertions over the parser, against synthetic registries
//
// The SWIFT registry TXT cannot be committed, so the fixtures invent their countries on ISO 3166
// user-assigned codes, which no real registry revision can ever hand out. scripts/testdata/README.md
// says which quirk each country stands for; keep the two in step when either changes.
// ---------------------------------------------------------------------------

fun selfCheck() {
    var checks = 0

    fun expect(what: String, expected: Any?, actual: Any?) {
        check(expected == actual) { "$what: expected <$expected>, got <$actual>" }
        checks++
    }

    fun expectRejected(what: String, needle: String, vararg countries: Country) {
        val message = runCatching { validate(countries.toList()) }.exceptionOrNull()?.message
        check(message != null && needle in message) {
            "$what: expected validation to fail with <$needle>, got <${message ?: "no failure"}>"
        }
        checks++
    }

    fun expectAccepted(what: String, vararg countries: Country) {
        val message = runCatching { validate(countries.toList()) }.exceptionOrNull()?.message
        check(message == null) { "$what: expected validation to pass, got <$message>" }
        checks++
    }

    // Position ranges. Offsets are into the whole IBAN, so a "1-4" BBAN range starts at 4.
    expect("a BBAN range", IdentifierPosition(4, 8), IdentifierPosition.ofBban("1-4"))
    expect("a BBAN range padded with spaces", IdentifierPosition(8, 11), IdentifierPosition.ofBban(" 5-7 "))
    expect("a two-digit BBAN range", IdentifierPosition(13, 16), IdentifierPosition.ofBban("10-12"))
    expect("a single-character BBAN range", IdentifierPosition(4, 5), IdentifierPosition.ofBban("1-1"))
    expect("a BBAN range stated as N/A", IdentifierPosition.ABSENT, IdentifierPosition.ofBban("N/A"))
    expect("an empty BBAN range", IdentifierPosition.ABSENT, IdentifierPosition.ofBban(""))
    expect("the length of a BBAN range", 4, IdentifierPosition.ofBban("1-4").length)
    expect("an absent range is not present", false, IdentifierPosition.ABSENT.present)
    expect("a parsed range is present", true, IdentifierPosition.ofBban("1-4").present)
    expect("cutting an absent identifier", null, IdentifierPosition.ABSENT.cutFrom("XA86BANK123456789012"))
    expect("cutting a present identifier", "BANK", IdentifierPosition.ofBban("1-4").cutFrom("XA86BANK123456789012"))

    expect("mod-97 of a sound IBAN", 1, mod97("XA86BANK123456789012"))
    expect("mod-97 of an IBAN with a wrong check digit", 2, mod97("XA87BANK123456789012"))

    expect("pretty-printing a length divisible by four", "XA86 BANK 1234 5678 9012", pretty("XA86BANK123456789012"))
    expect("pretty-printing a length that is not", "XF24 FBNK 0070 000", pretty("XF24FBNK0070000"))

    // Parsing the fixture. A row is one logical record however many lines its quoted cells span;
    // splitting one in two would misalign every column after it.
    expect("rows in the fixture", 17, parseTsv(syntheticRegistry).size)

    val parsed = parseRegistry(syntheticRegistry)
    val byCode = parsed.associateBy(Country::code)
    expect("countries in the order the fixture states them", listOf("XE", "XA", "XD", "XC", "XB", "XF"), parsed.map(Country::code))
    expect("every registry entry counts as a SWIFT entry", true, parsed.all(Country::swift))
    expect("SEPA membership", listOf(false, true, false, true, false, true), parsed.map(Country::sepa))
    expect("declared IBAN lengths", listOf(24, 20, 16, 22, 18, 15), parsed.map(Country::length))
    expect("a name padded with spaces", "Whitespace Republic", byCode.getValue("XC").name)
    expect("an example IBAN written in print format", "XC55WSPC77123456789012", byCode.getValue("XC").example)
    expect("a bank identifier position", IdentifierPosition(4, 9), byCode.getValue("XE").bank)
    expect("a branch identifier position", IdentifierPosition(9, 12), byCode.getValue("XE").branch)
    expect("a bank identifier position padded with spaces", IdentifierPosition(4, 8), byCode.getValue("XC").bank)
    expect("a bank identifier the country does not embed", IdentifierPosition.ABSENT, byCode.getValue("XD").bank)
    expect("a branch identifier the country does not embed", IdentifierPosition.ABSENT, byCode.getValue("XB").branch)
    expect("an identifier example written with spaces", "WSPC", byCode.getValue("XC").bankExample)
    expect("a branch identifier example written with spaces", "77", byCode.getValue("XC").branchExample)
    expect("a branch identifier example stated as N/A", "N/A", byCode.getValue("XE").branchExample)
    // The fixture's "Branch identifier example" row stops one column short, as a truncated
    // download would: the missing value has to read as absent rather than shift the row.
    expect("a value missing from a short row", "", byCode.getValue("XF").branchExample)
    expect("the country after a short row's last value", "XF24FBNK0070000", byCode.getValue("XF").example)

    // A whole row can go missing too - a registry revision that renames a column takes it away from
    // every country at once, and the fields it carried have to read as absent rather than throw.
    val minimal = parseRegistry(minimalRegistry)
    expect("countries in a registry stripped to its mandatory rows", listOf("XG", "XH"), minimal.map(Country::code))
    expect("a name still read from a stripped registry", "Sparseland", minimal.first().name)
    expect("SEPA membership without a SEPA row", listOf(false, false), minimal.map(Country::sepa))
    expect("identifier positions without a position row", listOf(IdentifierPosition.ABSENT, IdentifierPosition.ABSENT), minimal.map(Country::bank))
    expect("identifier examples without an example row", listOf("", ""), minimal.map(Country::branchExample))
    expectAccepted("a registry stripped to its mandatory rows", *merge(minimal).toTypedArray())

    // Merging in the curated overlay.
    val merged = merge(parsed)
    expect("merging appends the experimental overlay", parsed.size + experimentalCountries.size, merged.size)
    expect("merging sorts by country code", merged.map(Country::code).sorted(), merged.map(Country::code))
    val overlaid = merged.first { it.code == experimentalCountries.first().code }
    expect("an overlay entry is not in the registry", false, overlaid.swift)
    expect("an overlay entry embeds no bank identifier", IdentifierPosition.ABSENT, overlaid.bank)
    expect("an overlay entry takes its length from its example", overlaid.example.length, overlaid.length)
    // The SEPA overrides name real countries, so no invented code can carry one; borrow a code.
    val overridden = byCode.getValue("XE").copy(code = sepaOverrides.first(), sepa = false)
    expect("SEPA is overridden for a country the registry does not flag", true, merge(listOf(overridden)).single { it.code == overridden.code }.sepa)
    expect("SEPA is left alone outside the overrides", false, merge(listOf(byCode.getValue("XE"))).single { it.code == "XE" }.sepa)
    val caughtUp = runCatching { merge(listOf(byCode.getValue("XE").copy(code = experimentalCountries.first().code))) }
    expect(
        "merging a registry that has caught up with the overlay",
        true,
        caughtUp.exceptionOrNull()?.message?.contains("is now in the SWIFT registry") == true,
    )

    // Validation, which is what stands between a garbled download and the library.
    expectAccepted("the synthetic registry as a whole", *merged.toTypedArray())
    val sound = byCode.getValue("XA")
    expectRejected("an example with the wrong country prefix", "has wrong prefix", sound.copy(example = "XZ${sound.example.drop(2)}"))
    expectRejected("an example that is not the declared length", "example length 20 != declared 21", sound.copy(length = 21))
    expectRejected("an example with a wrong check digit", "fails mod-97 check", sound.copy(example = "XA87BANK123456789012"))
    // Positions past the end of the IBAN have to be reported, not cut from it.
    expectRejected("a bank identifier reaching past the IBAN", "identifier positions out of range", sound.copy(bank = IdentifierPosition(4, 21)))
    expectRejected("a bank identifier position that runs backwards", "identifier positions out of range", sound.copy(bank = IdentifierPosition(8, 4)))
    expectRejected("a bank identifier the position data disagrees with", "bank id by position 'BANK' != registry example 'WRONG'", sound.copy(bankExample = "WRONG"))
    expectRejected("a branch identifier the position data disagrees with", "branch id by position '123' != registry example '999'", sound.copy(branchExample = "999"))

    // "BA" is one of the countries knownIdentifierExampleMismatches exempts; the list holds only as
    // long as it does, so a revision that fixes one has to fail rather than silently keep it.
    val exempt = sound.copy(code = "BA", example = "BA90BANK123456789012")
    expectAccepted("an exempt country whose registry examples still disagree", exempt.copy(bankExample = "WRONG"))
    expectRejected("an exempt country whose registry examples now agree", "remove the exception", exempt)

    println("All $checks self-checks passed.")
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

if (args.contains("--self-check")) selfCheck() else generate()
