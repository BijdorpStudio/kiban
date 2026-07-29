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
        if (!c.example.startsWith(c.code)) problems += "${c.code}: example ${c.example} has wrong prefix"
        if (c.example.length != c.length) problems += "${c.code}: example length ${c.example.length} != declared ${c.length}"
        if (mod97(c.example) != 1) problems += "${c.code}: example ${c.example} fails mod-97 check"
        if (c.bank.begin > c.bank.end || c.bank.end > c.length || c.branch.begin > c.branch.end || c.branch.end > c.length) {
            problems += "${c.code}: identifier positions out of range"
        }
        // Cross-check: identifiers cut by position from the example IBAN must equal the
        // registry's independently stated identifier examples.
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

fun generatedKdoc(what: String) =
    "$what This is a generated file, do not edit manually.\n$regenerateNote\nUpdated to SWIFT IBAN Registry version $rev on $date.\n"

fun constInt(name: String, expression: String, kdoc: String? = null): PropertySpec =
    PropertySpec.builder(name, INT, KModifier.CONST)
        .apply { kdoc?.let(::addKdoc) }
        .initializer(expression)
        .build()

fun dataFileSpec(countries: List<Country>): FileSpec {
    val codesInitializer = buildCodeBlock {
        add("arrayOf(\n")
        indent()
        countries.forEach { add("%S,\n", it.code) }
        unindent()
        add(")")
    }
    val lengthsInitializer = buildCodeBlock {
        add("intArrayOf(\n")
        indent()
        countries.forEach { c ->
            val flags = (if (c.swift) " or SWIFT" else "") + (if (c.sepa) " or SEPA" else "")
            add("/* %L */ %L%L,\n", c.code, c.length, flags)
        }
        unindent()
        add(")")
    }
    val bankBranchInitializer = buildCodeBlock {
        add("intArrayOf(\n")
        indent()
        countries.forEach { c ->
            add("/* %L */ %L\n", c.code, c.bank.begin)
            indent()
            indent()
            add("or ((%L + %L) shl BANK_IDENTIFIER_END_SHIFT)\n", c.bank.begin, c.bank.length)
            add("or (%L shl BRANCH_IDENTIFIER_BEGIN_SHIFT)\n", c.branch.begin)
            add("or ((%L + %L) shl BRANCH_IDENTIFIER_END_SHIFT),\n", c.branch.begin, c.branch.length)
            unindent()
            unindent()
        }
        unindent()
        add(")")
    }

    val dataObject = TypeSpec.objectBuilder("CountryCodesData")
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(generatedKdoc("Contains information about IBAN country codes."))
        .addProperty(
            PropertySpec.builder("LAST_UPDATE_DATE", STRING, KModifier.CONST)
                .addKdoc("The \"yyyy-MM-dd\" datestamp that the embedded IBAN data was updated.\n")
                .initializer("%S", date)
                .build()
        )
        .addProperty(
            PropertySpec.builder("LAST_UPDATE_REV", STRING, KModifier.CONST)
                .addKdoc("The revision of the SWIFT IBAN Registry to which the embedded IBAN data was updated.\n")
                .initializer("%S", rev)
                .build()
        )
        .addProperty(constInt("SEPA", "1 shl 8"))
        .addProperty(constInt("SWIFT", "1 shl 9"))
        .addProperty(constInt("REMOVE_METADATA_MASK", "0xFF"))
        .addProperty(constInt("BANK_IDENTIFIER_BEGIN_MASK", "0xFF"))
        .addProperty(constInt("BANK_IDENTIFIER_END_SHIFT", "8"))
        .addProperty(constInt("BANK_IDENTIFIER_END_MASK", "0xFF shl BANK_IDENTIFIER_END_SHIFT"))
        .addProperty(constInt("BRANCH_IDENTIFIER_BEGIN_SHIFT", "16"))
        .addProperty(constInt("BRANCH_IDENTIFIER_BEGIN_MASK", "0xFF shl BRANCH_IDENTIFIER_BEGIN_SHIFT"))
        .addProperty(constInt("BRANCH_IDENTIFIER_END_SHIFT", "24"))
        .addProperty(constInt("BRANCH_IDENTIFIER_END_MASK", "0xFF shl BRANCH_IDENTIFIER_END_SHIFT"))
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

fun testFileSpec(countries: List<Country>): FileSpec {
    val testDataType = ClassName(pkg, "IbanCountryTestData")
    val entriesInitializer = buildCodeBlock {
        add("listOf(\n")
        indent()
        countries
            .sortedWith(compareBy({ !it.swift }, { if (it.swift) it.code else it.name }))
            .forEach { c ->
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
                add("),\n")
            }
        unindent()
        add(")")
    }

    val property = PropertySpec.builder("countryTestData", LIST.parameterizedBy(testDataType), KModifier.INTERNAL)
        .addKdoc(
            generatedKdoc("Valid example IBANs for every known country.") +
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
// Main
// ---------------------------------------------------------------------------

val countries = merge(parseRegistry(File(registryPath)))
validate(countries)
dataFile.writeText(licenseHeader(date.substring(0, 4)) + dataFileSpec(countries))
testFile.writeText(licenseHeader(date.substring(0, 4)) + testFileSpec(countries))
val swiftCount = countries.count(Country::swift)
println("Generated data for ${countries.size} countries ($swiftCount SWIFT, ${countries.size - swiftCount} experimental) at registry rev $rev.")
println("Wrote ${dataFile.relativeTo(repoRoot)} and ${testFile.relativeTo(repoRoot)}.")
