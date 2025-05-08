package nl.bijdorpstudio.kiban

import java.util.Optional

object IBANFields {
    fun getBankIdentifier(iban: Iban): Optional<String> =
        Optional.ofNullable(CountryCodes.getBankIdentifier(iban))

    fun getBranchIdentifier(iban: Iban): Optional<String> =
        Optional.ofNullable(CountryCodes.getBranchIdentifier(iban))
}