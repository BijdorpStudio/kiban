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
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.tableOf
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

/** Miscellaneous tests for the [Iban] class. */
class IbanTest {
    @Test
    fun `Operator invoke should parse IBAN`() {
        // Called through the companion explicitly: inside the library module the internal
        // constructor would win.
        val iban = Iban.Companion.invoke(VALID_IBAN).getOrThrow()
        assertThat(iban.plain).isEqualTo(VALID_IBAN)
    }

    @Test
    fun `toIban extension should parse IBAN`() {
        val iban = VALID_IBAN.toIban().getOrThrow()
        assertThat(iban.plain).isEqualTo(VALID_IBAN)
    }

    @Test
    fun `toIban extension should return failure for invalid IBAN`() {
        assertThat(INVALID_IBAN.toIban().failure()).isInstanceOf<IbanParseException.WrongChecksum>()
    }

    @Test
    fun `toIbanOrNull extension should parse IBAN`() {
        assertThat(VALID_IBAN.toIbanOrNull()?.plain).isEqualTo(VALID_IBAN)
    }

    @Test
    fun `toIbanOrNull extension should return null for invalid IBAN`() {
        assertThat(INVALID_IBAN.toIbanOrNull()).isNull()
    }

    @Test
    fun `isValidIban extension should return true for valid IBAN`() {
        assertThat(VALID_IBAN.isValidIban()).isTrue()
    }

    @Test
    fun `isValidIban extension should return false for invalid IBAN`() {
        assertThat(INVALID_IBAN.isValidIban()).isFalse()
    }

    @Test
    fun `isValidIban and toIbanOrNull should agree with parse for every rejection kind`() {
        listOf(
                "",
                "Shenanigans!",
                " $VALID_IBAN",
                "$VALID_IBAN ",
                "NL03",
                "NLAB0143267469",
                VALID_IBAN.replaceRange(6, 7, "_"),
                "UU345678345543234",
                VALID_IBAN + "0",
                INVALID_IBAN,
                VALID_IBAN,
            )
            .forEach { input ->
                val expectedSuccess = Iban.parse(input).isSuccess

                assertThat(input.isValidIban(), "isValidIban for '$input'")
                    .isEqualTo(expectedSuccess)
                assertThat(input.toIbanOrNull() != null, "toIbanOrNull for '$input'")
                    .isEqualTo(expectedSuccess)
            }
    }

    @Test
    fun `Valid IBAN should return country code`() {
        assertThat(Iban.parse(VALID_IBAN).getOrThrow().countryCode).isEqualTo("NL")
    }

    @Test
    fun `Valid IBAN should return check digits`() {
        assertThat(Iban.parse(VALID_IBAN).getOrThrow().checkDigits).isEqualTo("03")
    }

    @Test
    fun `valueOf should accept toString output of valid IBAN`() {
        val original = Iban.parse(VALID_IBAN).getOrThrow()
        val copy = Iban.valueOf(original.toString())
        assertThat(copy).isEqualTo(original)
    }

    @Test
    fun `valueOf should throw for invalid input`() {
        assertFailsWith<IbanParseException.WrongChecksum> {
            Iban.valueOf(INVALID_IBAN)
        }
    }

    @Test
    fun `Parse should reject empty input`() {
        assertThat(Iban.parse("").malformedKind())
            .isEqualTo(IbanParseException.Malformed.Kind.EMPTY)
    }

    @Test
    fun `Parse should reject invalid input`() {
        val failure = Iban.parse("Shenanigans!").failure()

        assertThat((failure as? IbanParseException.Malformed)?.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
        assertThat(failure.message)
            .isEqualTo("Input begins or ends in an invalid character: Shenanigans!")
    }

    @Test
    fun `Parse should reject leading whitespace`() {
        val failure = Iban.parse(" $VALID_IBAN").failure()

        assertThat((failure as? IbanParseException.Malformed)?.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
        assertThat(failure.message)
            .isEqualTo("Input begins or ends in an invalid character:  $VALID_IBAN")
    }

    @Test
    fun `Parse should reject trailing whitespace`() {
        val failure = Iban.parse("$VALID_IBAN ").failure()

        assertThat((failure as? IbanParseException.Malformed)?.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
        assertThat(failure.message)
            .isEqualTo("Input begins or ends in an invalid character: $VALID_IBAN ")
    }

    @Test
    fun `Parse should reject too short input`() {
        val failure = Iban.parse("NL03").failure()

        assertThat((failure as? IbanParseException.Malformed)?.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.TOO_SHORT)
        assertThat(failure.message).isEqualTo("Length is too short to be an IBAN: NL03")
    }

    @Test
    fun `Parse should reject non numeric check digits`() {
        val failure = Iban.parse("NLAB0143267469").failure()

        assertThat((failure as? IbanParseException.Malformed)?.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.NON_NUMERIC_CHECK_DIGITS)
        assertThat(failure.message)
            .isEqualTo("Characters at index 2 and 3 not both numeric. NLAB0143267469")
    }

    @Test
    fun `Parse should reject unsupported characters`() {
        val invalidCharacter = VALID_IBAN.replaceRange(6, 7, "_")

        assertThat(Iban.parse(invalidCharacter).malformedKind())
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_CHARACTER)
    }

    @Test
    fun `Parse should reject unknown country code`() {
        val failure = Iban.parse("UU345678345543234").failure()

        assertThat(failure)
            .isInstanceOf<IbanParseException.UnknownCountryCode>()
            .prop(IbanParseException.UnknownCountryCode::countryCode)
            .isEqualTo("UU")
        assertThat(failure.message).isEqualTo("Unknown country code: UU")
    }

    @Test
    fun `Parse should reject wrong length`() {
        val tooLong = VALID_IBAN + "0"

        assertThat(Iban.parse(tooLong).failure())
            .isInstanceOf<IbanParseException.WrongLength>()
            .prop(IbanParseException.WrongLength::expectedLength)
            .isEqualTo(18)
    }

    @Test
    fun `Parse should reject checksum failure`() {
        val failure = Iban.parse(INVALID_IBAN).failure()

        assertThat(failure)
            .isInstanceOf<IbanParseException.WrongChecksum>()
            .prop(IbanParseException.WrongChecksum::input)
            .isEqualTo(INVALID_IBAN)
        assertThat(failure.message).isEqualTo("Wrong check sum for $INVALID_IBAN")
    }

    @Test
    fun `Parse failure should be an IllegalArgumentException`() {
        assertThat(Iban.parse(INVALID_IBAN).failure()).isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `Compose should handle correct input`() {
        val composed =
            Iban.compose(
                countryCode = VALID_IBAN.substring(0, 2),
                bban = VALID_IBAN.substring(4),
            )
        assertThat(composed.getOrThrow()).isEqualTo(Iban.parse(VALID_IBAN).getOrThrow())
    }

    @Test
    fun `Compose should handle check digits of ten and higher`() {
        val composed = Iban.compose(countryCode = "BI", bban = "10000100010000332045181")

        assertThat(composed.getOrThrow().plain).isEqualTo("BI4210000100010000332045181")
    }

    @Test
    fun `Compose should reject blank country code`() {
        assertThat(Iban.compose(countryCode = "  ", bban = VALID_IBAN.substring(4)).failure())
            .isInstanceOf<IbanParseException>()
    }

    @Test
    fun `Compose should reject malformed country code`() {
        assertThat(
                Iban.compose(countryCode = "potato", bban = VALID_IBAN.substring(4)).malformedKind()
            )
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_STRUCTURE)
    }

    @Test
    fun `Compose should reject unknown country code`() {
        assertThat(Iban.compose(countryCode = "XX", bban = VALID_IBAN.substring(4)).failure())
            .isInstanceOf<IbanParseException.UnknownCountryCode>()
    }

    @Test
    fun `Compose should reject wrong length BBAN`() {
        assertThat(
                Iban.compose(
                        countryCode = VALID_IBAN.substring(0, 2),
                        bban = VALID_IBAN.substring(5),
                    )
                    .failure()
            )
            .isInstanceOf<IbanParseException.WrongLength>()
    }

    @Test
    fun `Equals contract should be satisfied`() {
        val x = Iban.parse(VALID_IBAN).getOrThrow()
        val y = Iban.parse(VALID_IBAN).getOrThrow()
        val z = Iban.parse(VALID_IBAN).getOrThrow()

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
        val expected =
            listOf(
                Iban.parse("DK3400000000000003").getOrThrow(),
                Iban.parse("NL41BANK0000000002").getOrThrow(),
                Iban.parse("NL68BANK0000000001").getOrThrow(),
            )
        val actual =
            listOf(
                Iban.parse("NL68BANK0000000001").getOrThrow(),
                Iban.parse("DK3400000000000003").getOrThrow(),
                Iban.parse("NL41BANK0000000002").getOrThrow(),
            )

        assertThat(actual.sorted()).isEqualTo(expected)
    }

    companion object {
        internal const val VALID_IBAN = "NL03ABNA0143267469"
        private const val INVALID_IBAN = "NL13ABNA0143267469"

        private fun Result<Iban>.failure(): Throwable =
            exceptionOrNull() ?: fail("Expected a failure, but was $this")

        private fun Result<Iban>.malformedKind(): IbanParseException.Malformed.Kind? =
            (exceptionOrNull() as? IbanParseException.Malformed)?.kind
    }
}
