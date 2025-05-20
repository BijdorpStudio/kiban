package nl.garvelink.oss.iban

import nl.bijdorpstudio.kiban.CountryCodes as KibanCountryCodes

/**
 * This object provides compatibility with the java-iban library API but is implemented using kiban.
 * Please migrate to nl.bijdorpstudio.kiban.CountryCodes.
 */
@Deprecated(
    message = "This object provides compatibility with the java-iban library API but is implemented using kiban. Please migrate to nl.bijdorpstudio.kiban.CountryCodes.",
)
object CountryCodes {
    @Deprecated(
        message = "Migrate to kiban's CountryCodes.getLengthForCountryCode and handle potential null return.",
        replaceWith = ReplaceWith(
            "CountryCodes.getLengthForCountryCode(countryCode)",
            "nl.bijdorpstudio.kiban.CountryCodes"
        )
    )
    @JvmStatic
    fun getLengthForCountryCode(countryCode: String): Int = KibanCountryCodes.getLengthForCountryCode(countryCode)
}
