package nl.garvelink.oss.iban

import nl.bijdorpstudio.kiban.IbanFields as KibanIbanFields
import nl.garvelink.oss.iban.IBAN // Our own compatibility IBAN
import java.util.Optional

/**
 * Provides utility methods to extract specific fields from an IBAN, such as bank or branch identifiers,
 * based on country-specific rules.
 * This object provides compatibility with the java-iban library API but is implemented using kiban.
 * Please migrate to nl.bijdorpstudio.kiban.IbanFields.
 */
@Deprecated(
    message = "This object provides compatibility with the java-iban library API but is implemented using kiban. Please migrate to nl.bijdorpstudio.kiban.IbanFields.",
    replaceWith = ReplaceWith("IbanFields", "nl.bijdorpstudio.kiban.IbanFields")
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
        replaceWith = ReplaceWith("KibanIbanFields.getBankIdentifier(iban.actualKibanIban)", "nl.bijdorpstudio.kiban.IbanFields as KibanIbanFields")
    )
    @JvmStatic
    fun getBankIdentifier(iban: IBAN): Optional<String> {
        return KibanIbanFields.getBankIdentifier(iban.actualKibanIban)
    }

    /**
     * Extracts the branch identifier from the given IBAN, if defined for the IBAN's country.
     *
     * @param iban The IBAN from which to extract the branch identifier.
     * @return An {@code Optional<String>} containing the branch identifier, or empty if not applicable.
     */
    @Deprecated(
        message = "Migrate to kiban's IbanFields.getBranchIdentifier",
        replaceWith = ReplaceWith("KibanIbanFields.getBranchIdentifier(iban.actualKibanIban)", "nl.bijdorpstudio.kiban.IbanFields as KibanIbanFields")
    )
    @JvmStatic
    fun getBranchIdentifier(iban: IBAN): Optional<String> {
        return KibanIbanFields.getBranchIdentifier(iban.actualKibanIban)
    }
}
