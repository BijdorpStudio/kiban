/*
   Copyright 2021 Barend Garvelink, Eugen Martynov

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package nl.bijdorpstudio.kiban

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.prop
import assertk.tableOf
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith


/**
 * Miscellaneous tests for the [Iban] class.
 */
class IbanTest {
    @Test
    fun `Valid IBAN should return country code`() {
        assertThat(Iban.parse(VALID_IBAN).countryCode).isEqualTo("NL")
    }

    @Test
    fun `Valid IBAN should return check digits`() {
        assertThat(Iban.parse(VALID_IBAN).checkDigits).isEqualTo("03")
    }

    @Test
    fun `valueOf should accept toString output of valid IBAN`() {
        val original = Iban.parse(VALID_IBAN)
        val copy = Iban.valueOf(original.toString())
        assertThat(copy).isEqualTo(original)
    }

    @Test
    fun `Parse should reject invalid input`() {
        assertFailsWith<IllegalArgumentException> {
            Iban.parse("Shenanigans!")
        }
    }

    @Test
    fun `Parse should reject leading whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            Iban.parse(" $VALID_IBAN")
        }
    }

    @Test
    fun `Parse should reject trailing whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            Iban.parse("$VALID_IBAN ")
        }
    }

    @Test
    fun `Parse should reject unknown country code`() {
        val exception = assertFails {
            Iban.parse("UU345678345543234")
        }

        assertThat(exception)
            .isInstanceOf<IllegalArgumentException>()
            .prop(IllegalArgumentException::message)
            .isEqualTo("Unknown country code: UU")
    }

    @Test
    fun `Parse should reject checksum failure`() {
        val exception = assertFails {
            Iban.parse(INVALID_IBAN)
        }

        assertThat(exception)
            .isInstanceOf<IllegalArgumentException>()
            .prop(IllegalArgumentException::message)
            .isEqualTo("Wrong check sum for $INVALID_IBAN")
    }

    @Test
    fun `Compose should handle correct input`() {
        val composed = Iban.compose(
            countryCode = VALID_IBAN.substring(0, 2),
            bban = VALID_IBAN.substring(4)
        )
        assertThat(composed).isEqualTo(Iban.parse(VALID_IBAN))
    }

    @Test
    fun `Compose should reject blank country code`() {
        assertFailsWith<IllegalArgumentException> {
            Iban.compose(
                countryCode = "  ",
                bban = VALID_IBAN.substring(4)
            )
        }
    }

    @Test
    fun `Compose should reject malformed country code`() {
        assertFailsWith<IllegalArgumentException> {
            Iban.compose(
                countryCode = "potato",
                bban = VALID_IBAN.substring(4)
            )
        }
    }

    @Test
    fun `Compose should reject unknown country code`() {
        assertFailsWith<IllegalArgumentException> {
            Iban.compose(
                countryCode = "XX",
                bban = VALID_IBAN.substring(4)
            )
        }
    }

    @Test
    fun `Compose should reject wrong length BBAN`() {
        assertFailsWith<IllegalArgumentException> {
            Iban.compose(
                countryCode = VALID_IBAN.substring(0, 2),
                bban = VALID_IBAN.substring(5)
            )
        }
    }

    @Test
    fun `Equals contract should be satisfied`() {
        val x = Iban.parse(VALID_IBAN)
        val y = Iban.parse(VALID_IBAN)
        val z = Iban.parse(VALID_IBAN)

        assertThat(x, "An object is not equal to nul").isNotEqualTo(null)
        assertThat(x, "An object equals itself").isEqualTo(x)
        assertThat(x, "Equality is symmetric and transitive").isEqualTo(y)
        assertThat(y, "Equality is symmetric").isEqualTo(x)
        assertThat(y, "Equality is transitive").isEqualTo(z)
        assertThat(x, "Equality is transitive").isEqualTo(z)
    }

    @Test
    fun `To pretty should format IBAN correctly`() {
        tableOf("input", "formatted")
            .row("", "")
            .row("12", "12")
            .row("1 2", "12")
            .row("1234", "1234")
            .row("1 2 3 4", "1234")
            .row("12345", "1234 5")
            .row("1234 5", "1234 5")
            .row("12345678", "1234 5678")
            .row("1234 5678", "1234 5678")
            .row("123456789", "1234 5678 9")
            .row("1234 5678 9", "1234 5678 9")
            .forAll { input, formatted ->
                assertThat(Iban.toPretty(input)).isEqualTo(formatted)
            }
    }

    @Test
    fun `To plain should remove formatting from IBAN`() {
        tableOf("input", "plain")
            .row("", "")
            .row("12", "12")
            .row("1 2", "12")
            .row("1234", "1234")
            .row("1 2 3 4", "1234")
            .row("12345", "12345")
            .row("1234 5", "12345")
            .row("12345678", "12345678")
            .row("1234 5678", "12345678")
            .row("123456789", "123456789")
            .row("1234 5678 9", "123456789")
            .forAll { input, plain ->
                assertThat(Iban.toPlain(input)).isEqualTo(plain)
            }
    }

    @Test
    fun `Lexical sort should order IBANs correctly`() {
        val expected = listOf(
            Iban.parse("DK3400000000000003"),
            Iban.parse("NL41BANK0000000002"),
            Iban.parse("NL68BANK0000000001")
        )
        val actual = listOf(
            Iban.parse("NL68BANK0000000001"),
            Iban.parse("DK3400000000000003"),
            Iban.parse("NL41BANK0000000002")
        )

        assertThat(actual.sorted()).isEqualTo(expected)
    }

    companion object {
        internal const val VALID_IBAN = "NL03ABNA0143267469"
        private const val INVALID_IBAN = "NL13ABNA0143267469"
    }
}
