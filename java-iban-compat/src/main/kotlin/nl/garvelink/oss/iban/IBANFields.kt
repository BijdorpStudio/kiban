package nl.garvelink.oss.iban

import nl.bijdorpstudio.kiban.CountryCodes
import java.util.Optional

/**
 * This object provides compatibility with the java-iban library API but is implemented using kiban.
 * Please migrate to nl.bijdorpstudio.kiban.IbanFields.
 */
@Deprecated(
    message = "This object provides compatibility with the java-iban library API but is implemented using kiban. Please migrate to nl.bijdorpstudio.kiban.CountryCodes but not using java optional.",
)
object IBANFields {

    /**
     * Extracts the bank identifier from the given IBAN, if defined for the IBAN's country.
     *
     * @param iban The IBAN from which to extract the bank identifier.
     * @return An {@code Optional<String>} containing the bank identifier, or empty if not applicable.
     */
    @Deprecated(
        message = "Migrate to kiban's IbanFields.getBankIdentifier",
        replaceWith = ReplaceWith(
            "CountryCodes.getBankIdentifier(iban)",
            "nl.bijdorpstudio.kiban.CountryCodes"
        )
    )
    @JvmStatic
    fun getBankIdentifier(iban: IBAN): Optional<String> =
        Optional.ofNullable(CountryCodes.getBankIdentifier(iban.iban))

    /**
     * Extracts the branch identifier from the given IBAN, if defined for the IBAN's country.
     *
     * @param iban The IBAN from which to extract the branch identifier.
     * @return An {@code Optional<String>} containing the branch identifier, or empty if not applicable.
     */
    @Deprecated(
        message = "Migrate to kiban's CountryCodes.getBranchIdentifier",
        replaceWith = ReplaceWith(
            "CountryCodes.getBranchIdentifier(iban)",
            "nl.bijdorpstudio.kiban.CountryCodes"
        )
    )
    @JvmStatic
    fun getBranchIdentifier(iban: IBAN): Optional<String> =
        Optional.ofNullable(CountryCodes.getBranchIdentifier(iban.iban))
}
