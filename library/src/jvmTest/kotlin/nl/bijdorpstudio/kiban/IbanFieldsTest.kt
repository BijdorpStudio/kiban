package nl.bijdorpstudio.kiban

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test


class IbanFieldsTest {
    @Test
    fun `Should extract bank identifier`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                val iban = Iban.parse(td.plain)
                val bankIdentifier = IBANFields.getBankIdentifier(iban)
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
                val branchIdentifier = IBANFields.getBranchIdentifier(iban)
                if (td.branch == null) {
                    assertThat(branchIdentifier.isPresent).isFalse()
                } else {
                    assertThat(branchIdentifier.get()).isEqualTo(td.branch)
                }
            }
    }

    // This should fail lint
    @Test
    fun test1() {
       val test: Boolean? = true

        assertThat(test!!).isTrue()
    }
}