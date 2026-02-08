package nl.bijdorpstudio.kiban

import java.util.Optional

object IBANFields {
    @Deprecated("Use Iban.bankIdentifier instead", ReplaceWith("iban.bankIdentifier"))
    fun getBankIdentifier(iban: Iban): Optional<String> =
        Optional.ofNullable(iban.bankIdentifier)

    @Deprecated("Use Iban.branchIdentifier instead", ReplaceWith("iban.branchIdentifier"))
    fun getBranchIdentifier(iban: Iban): Optional<String> =
        Optional.ofNullable(iban.branchIdentifier)
}