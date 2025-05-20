package nl.garvelink.oss.iban

import nl.bijdorpstudio.kiban.CountryCodes as KibanCountryCodes

/**
 * Provides information about IBAN country codes, such as the expected length of an IBAN for a specific country.
 * This object provides compatibility with the java-iban library API but is implemented using kiban.
 * Please migrate to nl.bijdorpstudio.kiban.CountryCodes.
 */
@Deprecated(
    message = "This object provides compatibility with the java-iban library API but is implemented using kiban. Please migrate to nl.bijdorpstudio.kiban.CountryCodes.",
    replaceWith = ReplaceWith("CountryCodes", "nl.bijdorpstudio.kiban.CountryCodes")
)
object CountryCodes {

    /**
     * Returns the expected length of an IBAN for the given country code.
     * Throws an [IllegalArgumentException] if the country code is invalid or not found,
     * mirroring the behavior of the original java-iban library.
     *
     * @param countryCode The two-letter ISO 3166 country code.
     * @return The expected length.
     * @throws IllegalArgumentException if the country code is invalid or not found.
     */
    @Deprecated(
        message = "Migrate to kiban's CountryCodes.getLengthForCountryCode and handle potential null return.",
        replaceWith = ReplaceWith("CountryCodes.getLengthForCountryCode(countryCode)", "nl.bijdorpstudio.kiban.CountryCodes")
    )
    @JvmStatic
    fun getLengthForCountryCode(countryCode: String): Int {
        return KibanCountryCodes.getLengthForCountryCode(countryCode)
            ?: throw IllegalArgumentException("Invalid country code: $countryCode")
    }
}
