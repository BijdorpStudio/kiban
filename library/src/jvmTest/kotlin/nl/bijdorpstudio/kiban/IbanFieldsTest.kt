package nl.bijdorpstudio.kiban

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import kotlin.test.Test


class IbanFieldsTest {
    @Test
    fun `Should extract bank identifier`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                val iban = Iban.parse(td.plain)
                val bankIdentifier = IbanFields.getBankIdentifier(iban)
                assertThat(bankIdentifier).isNotNull()
                if (td.bank == null) {
                    assertThat(bankIdentifier.isPresent).isFalse()
                } else {
                    assertThat(bankIdentifier.get()).isEqualTo(td.bank)
                }
            }
    }

    @Test
    fun `Should extract branch identifier`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                val iban = Iban.parse(td.plain)
                val branchIdentifier = IbanFields.getBranchIdentifier(iban)
                assertThat(branchIdentifier).isNotNull()
                if (td.branch == null) {
                    assertThat(branchIdentifier.isPresent).isFalse()
                } else {
                    assertThat(branchIdentifier.get()).isEqualTo(td.branch)
                }
            }
    }
}