package nl.bijdorpstudio.kiban.samples.jvmcli

import nl.bijdorpstudio.kiban.CountryCodes
import nl.bijdorpstudio.kiban.Iban
import nl.bijdorpstudio.kiban.IbanParseException
import nl.bijdorpstudio.kiban.Modulo97
import nl.bijdorpstudio.kiban.isValidIban
import nl.bijdorpstudio.kiban.toIban
import nl.bijdorpstudio.kiban.toIbanOrNull

fun main() {
    // The primary entry point. Throws IbanParseException on invalid input.
    val iban: Iban = Iban("NL91ABNA0417164300")
    println("invoke: $iban")

    // Or use the String extension; same throwing behaviour.
    val parsed: Iban = "NL91ABNA0417164300".toIban()
    println("toIban: $parsed")

    // Exception-free fast paths.
    val orNull: Iban? = "NL91ABNA0417164301".toIbanOrNull() // null, check digits are wrong
    println("toIbanOrNull (wrong check digits): $orNull")
    val isValid: Boolean = "NL91ABNA0417164300".isValidIban() // true
    println("isValidIban: $isValid")

    // Failures carry a typed reason, so you never have to match on messages.
    try {
        Iban("not an iban")
    } catch (failure: IbanParseException) {
        when (failure) {
            is IbanParseException.UnknownCountryCode ->
                println("unknown country: ${failure.countryCode}")
            is IbanParseException.WrongLength ->
                println(
                    "wrong length: expected ${failure.expectedLength}, got ${failure.actualLength}"
                )
            is IbanParseException.WrongChecksum -> println("wrong checksum")
            is IbanParseException.Malformed -> println("malformed: ${failure.kind}")
        }
    }

    // toString() emits standard formatting, plain is compact.
    println("formatted: ${iban.toString()}")
    println("plain: ${iban.plain}")

    // Input may be formatted.
    val anotherIban = Iban("BE68 5390 0754 7034")
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

    // Compose the IBAN for a country and BBAN; also throws on invalid input.
    val composed = Iban.compose("BI", "10000100010000332045181") // BI4210000100010000332045181
    println("compose: ${composed.plain}")

    // You can query whether an IBAN is of a SEPA-participating country
    val isSepa = Iban(candidate).isSEPA // true
    println("isSEPA: $isSepa")

    // You can query whether an IBAN is in the SWIFT Registry
    val isRegistered = Iban(candidate).isInSwiftRegistry // true
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
    val expectedLength: Int? = CountryCodes.ibanLength("DK") // 18
    println("ibanLength(DK): $expectedLength")

    // Get the Bank Identifier and Branch Identifier:
    println("bankIdentifier: ${iban.bankIdentifier}")
    println("branchIdentifier: ${iban.branchIdentifier}")
}
