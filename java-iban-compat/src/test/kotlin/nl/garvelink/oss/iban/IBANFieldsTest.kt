package nl.garvelink.oss.iban

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import kotlin.test.Test

class IBANFieldsTest {
    @Test
    fun `Should extract bank identifier for iban with`() {
        val iban = IBAN.parse("AD1200012030200359100100")
        val bankIdentifier = IBANFields.getBankIdentifier(iban)
        assertThat(bankIdentifier).isNotNull()
        assertThat(bankIdentifier.get()).isEqualTo("0001")
    }

    @Test
    fun `Should extract bank identifier for iban without`() {
        val iban = IBAN.parse("DZ580002100001113000000570")
        val bankIdentifier = IBANFields.getBankIdentifier(iban)
        assertThat(bankIdentifier).isNotNull()
        assertThat(bankIdentifier.isPresent).isFalse()
    }

    @Test
    fun `Should extract branch identifier with`() {
        val iban = IBAN.parse("AD1200012030200359100100")
        val branchIdentifier = IBANFields.getBranchIdentifier(iban)
        assertThat(branchIdentifier).isNotNull()
        assertThat(branchIdentifier.get()).isEqualTo("2030")
    }

    @Test
    fun `Should extract branch identifier without`() {
        val iban = IBAN.parse("AE070331234567890123456")
        val branchIdentifier = IBANFields.getBranchIdentifier(iban)
        assertThat(branchIdentifier).isNotNull()
        assertThat(branchIdentifier.isPresent).isFalse()
    }
}