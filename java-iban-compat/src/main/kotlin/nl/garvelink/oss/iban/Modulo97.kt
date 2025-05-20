package nl.garvelink.oss.iban

import nl.bijdorpstudio.kiban.Modulo97 as KibanModulo97

/**
 * This object provides compatibility with the java-iban library API but is implemented using kiban.
 * Please migrate to nl.bijdorpstudio.kiban.Modulo97.
 */
@Deprecated(
    message = "This object provides compatibility with the java-iban library API but is implemented using kiban. Please migrate to nl.bijdorpstudio.kiban.Modulo97.",
)
object Modulo97 {

    /**
     * Verifies the check digits of a potential IBAN string.
     *
     * @param candidate The IBAN string (or any string for Modulo-97 check).
     * @return `true` if the check digits are correct, `false` otherwise.
     */
    @Deprecated(
        message = "Migrate to kiban's Modulo97.verifyCheckDigits",
        replaceWith = ReplaceWith("Modulo97.verifyCheckDigits(candidate.toString())", "nl.bijdorpstudio.kiban.Modulo97")
    )
    @JvmStatic
    fun verifyCheckDigits(candidate: CharSequence): Boolean {
        return KibanModulo97.verifyCheckDigits(candidate.toString())
    }

    /**
     * Calculates the Modulo-97 check digits for a given string.
     * The input string is typically a country code followed by a BBAN.
     *
     * @param candidate The string for which to calculate check digits.
     * @return The calculated two-digit check sequence as an `Int`.
     */
    @Deprecated(
        message = "Migrate to kiban's Modulo97.calculateCheckDigits",
        replaceWith = ReplaceWith("Modulo97.calculateCheckDigits(candidate.toString())", "nl.bijdorpstudio.kiban.Modulo97")
    )
    @JvmStatic
    fun calculateCheckDigits(candidate: CharSequence): Int {
        return KibanModulo97.calculateCheckDigits(candidate.toString())
    }

    /**
     * Calculates the Modulo-97 check digits for a given country code and BBAN.
     *
     * @param countryCode The two-letter ISO 3166 country code.
     * @param bban The Basic Bank Account Number.
     * @return The calculated two-digit check sequence as an `Int`.
     */
    @Deprecated(
        message = "Migrate to kiban's Modulo97.calculateCheckDigits",
        replaceWith = ReplaceWith("Modulo97.calculateCheckDigits(countryCode, bban)", "nl.bijdorpstudio.kiban.Modulo97")
    )
    @JvmStatic
    fun calculateCheckDigits(countryCode: String, bban: String): Int {
        return KibanModulo97.calculateCheckDigits(countryCode, bban)
    }
}
