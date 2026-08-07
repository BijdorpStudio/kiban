package nl.bijdorpstudio.kiban.samples.jvmcli

import nl.bijdorpstudio.kiban.CountryCodes
import nl.bijdorpstudio.kiban.Iban
import nl.bijdorpstudio.kiban.IbanParseException
import nl.bijdorpstudio.kiban.Modulo97
import nl.bijdorpstudio.kiban.isValidIban
import nl.bijdorpstudio.kiban.toIban
import nl.bijdorpstudio.kiban.toIbanOrNull

/**
 * Runs through the kiban API the way README.md's "Use" section describes it, printing each result.
 * Not a test suite -- a runnable walkthrough of the library, depending on `:library` directly. Run
 * it with `./gradlew :samples:jvm-cli:run`.
 */
fun main() {
    // Parse returns Result<Iban>.
    val iban: Iban = Iban.parse("NL91ABNA0417164300").getOrThrow()
    println("parse: $iban")

    // Handle failure without exceptions.
    Iban.parse("not an iban")
        .fold(
            onSuccess = { println("fold success: $it") },
            onFailure = { println("fold failure: ${it.message}") },
        )

    // Or use the String extensions.
    val parsed: Result<Iban> = "NL91ABNA0417164300".toIban()
    println("toIban: $parsed")
    val orNull: Iban? = "NL91ABNA0417164301".toIbanOrNull() // null, check digits are wrong
    println("toIbanOrNull (wrong check digits): $orNull")
    val isValid: Boolean = "NL91ABNA0417164300".isValidIban() // true
    println("isValidIban: $isValid")

    // Failures carry a typed reason, so you never have to match on messages.
    when (val failure = Iban.parse("not an iban").exceptionOrNull()) {
        is IbanParseException.UnknownCountryCode ->
            println("unknown country: ${failure.countryCode}")
        is IbanParseException.WrongLength ->
            println("wrong length: expected ${failure.expectedLength}, got ${failure.actualLength}")
        is IbanParseException.WrongChecksum -> println("wrong checksum")
        is IbanParseException.Malformed -> println("malformed: ${failure.kind}")
        null -> println("parsed successfully")
    }

    // toString() emits standard formatting, plain is compact.
    println("formatted: ${iban.toString()}")
    println("plain: ${iban.plain}")

    // Input may be formatted.
    val anotherIban = Iban.parse("BE68 5390 0754 7034").getOrThrow()
    println("parsed formatted input: $anotherIban")

    // Iban implements Comparable<T>.
    val ibans = listOf(iban, anotherIban)
    println("sorted: ${ibans.sorted()}")

    // The equals() and hashCode() methods are implemented.
    val ibansAsKeys = mutableMapOf<Iban, String>()
    ibansAsKeys.put(iban, "this is fine")
    println("map lookup: ${ibansAsKeys[iban]}")

    // You can use the Modulo97 class directly to compute or verify the check digits on an input.
    val candidate = "GB29 NWBK 6016 1331 9268 19"
    val valid = Modulo97.verifyCheckDigits(candidate) // true
    println("verifyCheckDigits: $valid")

    // Compose the IBAN for a country and BBAN; this also returns a Result.
    val composed =
        Iban.compose("BI", "10000100010000332045181").getOrThrow() // BI4210000100010000332045181
    println("compose: ${composed.plain}")

    // You can query whether an IBAN is of a SEPA-participating country
    val isSepa = Iban.parse(candidate).getOrThrow().isSEPA // true
    println("isSEPA: $isSepa")

    // You can query whether an IBAN is in the SWIFT Registry
    val isRegistered = Iban.parse(candidate).getOrThrow().isInSwiftRegistry // true
    println("isInSwiftRegistry: $isRegistered")

    // Modulo97 API methods take CharSequence, not just String.
    val builder = StringBuilder("LU000019400644750000")
    val checkDigits = Modulo97.calculateCheckDigits(builder) // 28
    println("calculateCheckDigits(StringBuilder): $checkDigits")

    // Modulo97 API can calculate check digits, also for non-iban inputs.
    // It does assume/require that the check digits are on indices 2 and 3.
    println(
        "calculateCheckDigits(GB, ...): ${Modulo97.calculateCheckDigits("GB", "NWBK60161331926819")}"
    ) // 29
    println("calculateCheckDigits(XX, X): ${Modulo97.calculateCheckDigits("XX", "X")}") // 72

    // Get the expected IBAN length for a country code:
    val expectedLength: Int? = CountryCodes.getLength("DK") // 18
    println("getLength(DK): $expectedLength")

    // Get the Bank Identifier and Branch Identifier:
    println("bankIdentifier: ${iban.bankIdentifier}")
    println("branchIdentifier: ${iban.branchIdentifier}")
}
