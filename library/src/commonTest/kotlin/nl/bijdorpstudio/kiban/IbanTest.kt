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
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.tableOf
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Miscellaneous tests for the [Iban] class. */
class IbanTest {
    @Test
    fun `Operator invoke should parse IBAN`() {
        // Called through the companion explicitly: inside the library module the private
        // constructor would win over invoke for an exact String argument.
        val iban = Iban.Companion.invoke(VALID_IBAN)
        assertThat(iban.plain).isEqualTo(VALID_IBAN)
    }

    @Test
    fun `Operator invoke should throw for invalid input`() {
        assertFailsWith<IbanParseException.WrongChecksum> { Iban(INVALID_IBAN) }
    }

    @Test
    fun `toIban extension should parse IBAN`() {
        val iban = VALID_IBAN.toIban()
        assertThat(iban.plain).isEqualTo(VALID_IBAN)
    }

    @Test
    fun `toIban extension should throw for invalid IBAN`() {
        assertFailsWith<IbanParseException.WrongChecksum> { INVALID_IBAN.toIban() }
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
    fun `isValidIban and toIbanOrNull should agree with invoke for every rejection kind`() {
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
                val expectedSuccess = runCatching { Iban(input) }.isSuccess

                assertThat(input.isValidIban(), "isValidIban for '$input'")
                    .isEqualTo(expectedSuccess)
                assertThat(input.toIbanOrNull() != null, "toIbanOrNull for '$input'")
                    .isEqualTo(expectedSuccess)
            }
    }

    @Test
    fun `Valid IBAN should return country code`() {
        assertThat(Iban(VALID_IBAN).countryCode).isEqualTo("NL")
    }

    @Test
    fun `Valid IBAN should return check digits`() {
        assertThat(Iban(VALID_IBAN).checkDigits).isEqualTo("03")
    }

    @Test
    fun `Invoke should accept toString output of valid IBAN`() {
        val original = Iban(VALID_IBAN)
        val copy = Iban(original.toString())
        assertThat(copy).isEqualTo(original)
    }

    @Test
    fun `Invoke should reject empty input`() {
        assertThat(malformedKind { Iban("") }).isEqualTo(IbanParseException.Malformed.Kind.EMPTY)
    }

    @Test
    fun `Invoke should reject invalid input`() {
        val failure = assertFailsWith<IbanParseException.Malformed> { Iban("Shenanigans!") }

        assertThat(failure.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
        assertThat(failure.message)
            .isEqualTo("Input begins or ends in an invalid character: Shenanigans!")
    }

    @Test
    fun `Invoke should reject leading whitespace`() {
        val failure = assertFailsWith<IbanParseException.Malformed> { Iban(" $VALID_IBAN") }

        assertThat(failure.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
        assertThat(failure.message)
            .isEqualTo("Input begins or ends in an invalid character:  $VALID_IBAN")
    }

    @Test
    fun `Invoke should reject trailing whitespace`() {
        val failure = assertFailsWith<IbanParseException.Malformed> { Iban("$VALID_IBAN ") }

        assertThat(failure.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
        assertThat(failure.message)
            .isEqualTo("Input begins or ends in an invalid character: $VALID_IBAN ")
    }

    @Test
    fun `Invoke should reject too short input`() {
        val failure = assertFailsWith<IbanParseException.Malformed> { Iban("NL03") }

        assertThat(failure.kind).isEqualTo(IbanParseException.Malformed.Kind.TOO_SHORT)
        assertThat(failure.message).isEqualTo("Length is too short to be an IBAN: NL03")
    }

    @Test
    fun `Invoke should reject non numeric check digits`() {
        val failure = assertFailsWith<IbanParseException.Malformed> { Iban("NLAB0143267469") }

        assertThat(failure.kind)
            .isEqualTo(IbanParseException.Malformed.Kind.NON_NUMERIC_CHECK_DIGITS)
        assertThat(failure.message)
            .isEqualTo("Characters at index 2 and 3 not both numeric. NLAB0143267469")
    }

    @Test
    fun `Invoke should reject unsupported characters`() {
        val invalidCharacter = VALID_IBAN.replaceRange(6, 7, "_")

        assertThat(malformedKind { Iban(invalidCharacter) })
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_CHARACTER)
    }

    @Test
    fun `Invoke should reject unknown country code`() {
        val failure =
            assertFailsWith<IbanParseException.UnknownCountryCode> { Iban("UU345678345543234") }

        assertThat(failure).prop(IbanParseException.UnknownCountryCode::countryCode).isEqualTo("UU")
        assertThat(failure.message).isEqualTo("Unknown country code: UU")
    }

    @Test
    fun `Invoke should reject wrong length`() {
        val tooLong = VALID_IBAN + "0"

        val failure = assertFailsWith<IbanParseException.WrongLength> { Iban(tooLong) }

        assertThat(failure).prop(IbanParseException.WrongLength::expectedLength).isEqualTo(18)
    }

    @Test
    fun `Invoke should reject checksum failure`() {
        val failure = assertFailsWith<IbanParseException.WrongChecksum> { Iban(INVALID_IBAN) }

        assertThat(failure).prop(IbanParseException.WrongChecksum::input).isEqualTo(INVALID_IBAN)
        assertThat(failure.message).isEqualTo("Wrong check sum for $INVALID_IBAN")
    }

    @Test
    fun `Invoke failure should be an IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { Iban(INVALID_IBAN) }
    }

    @Test
    fun `Compose should handle correct input`() {
        val composed =
            Iban.compose(
                countryCode = VALID_IBAN.substring(0, 2),
                bban = VALID_IBAN.substring(4),
            )
        assertThat(composed).isEqualTo(Iban(VALID_IBAN))
    }

    @Test
    fun `Compose should handle check digits of ten and higher`() {
        val composed = Iban.compose(countryCode = "BI", bban = "10000100010000332045181")

        assertThat(composed.plain).isEqualTo("BI4210000100010000332045181")
    }

    @Test
    fun `Compose should reject blank country code`() {
        assertFailsWith<IbanParseException> {
            Iban.compose(countryCode = "  ", bban = VALID_IBAN.substring(4))
        }
    }

    @Test
    fun `Compose should reject malformed country code`() {
        assertThat(
                malformedKind {
                    Iban.compose(countryCode = "potato", bban = VALID_IBAN.substring(4))
                }
            )
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_STRUCTURE)
    }

    @Test
    fun `Compose should reject unknown country code`() {
        assertFailsWith<IbanParseException.UnknownCountryCode> {
            Iban.compose(countryCode = "XX", bban = VALID_IBAN.substring(4))
        }
    }

    @Test
    fun `Compose should reject wrong length BBAN`() {
        assertFailsWith<IbanParseException.WrongLength> {
            Iban.compose(countryCode = VALID_IBAN.substring(0, 2), bban = VALID_IBAN.substring(5))
        }
    }

    @Test
    fun `Equals contract should be satisfied`() {
        val x = Iban(VALID_IBAN)
        val y = Iban(VALID_IBAN)
        val z = Iban(VALID_IBAN)

        assertThat(x, "An object is not equal to nul").isNotEqualTo(null)
        assertThat(x, "An object equals itself").isEqualTo(x)
        assertThat(x, "Equality is symmetric and transitive").isEqualTo(y)
        assertThat(y, "Equality is symmetric").isEqualTo(x)
        assertThat(y, "Equality is transitive").isEqualTo(z)
        assertThat(x, "Equality is transitive").isEqualTo(z)
    }

    @Test
    fun `Add spaces should format IBAN correctly`() {
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
                assertThat(Iban.addSpaces(Iban.toPlain(input))).isEqualTo(formatted)
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
            .forAll { input, plain -> assertThat(Iban.toPlain(input)).isEqualTo(plain) }
    }

    @Test
    fun `Lexical sort should order IBANs correctly`() {
        val expected =
            listOf(
                Iban("DK3400000000000003"),
                Iban("NL41BANK0000000002"),
                Iban("NL68BANK0000000001"),
            )
        val actual =
            listOf(
                Iban("NL68BANK0000000001"),
                Iban("DK3400000000000003"),
                Iban("NL41BANK0000000002"),
            )

        assertThat(actual.sorted()).isEqualTo(expected)
    }

    // T3: for a representative set of invalid inputs across every rejection kind, invoke,
    // toIban and compose must only ever throw IbanParseException — nothing raw from Modulo97 or
    // the stdlib may escape. This is what makes @Throws sound on Kotlin/Native.
    @Test
    fun `Only IbanParseException escapes invoke and toIban for every rejection kind`() {
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
            )
            .forEach { input ->
                assertFailsWith<IbanParseException>("invoke($input)") { Iban(input) }
                assertFailsWith<IbanParseException>("toIban($input)") { input.toIban() }
            }
    }

    @Test
    fun `Only IbanParseException escapes compose for every rejection kind`() {
        assertFailsWith<IbanParseException> { Iban.compose("  ", VALID_IBAN.substring(4)) }
        assertFailsWith<IbanParseException> { Iban.compose("potato", VALID_IBAN.substring(4)) }
        assertFailsWith<IbanParseException> { Iban.compose("XX", VALID_IBAN.substring(4)) }
        assertFailsWith<IbanParseException> {
            Iban.compose(VALID_IBAN.substring(0, 2), VALID_IBAN.substring(5))
        }
    }

    companion object {
        internal const val VALID_IBAN = "NL03ABNA0143267469"
        private const val INVALID_IBAN = "NL13ABNA0143267469"

        private fun malformedKind(block: () -> Unit): IbanParseException.Malformed.Kind? =
            (assertFailsWith<IbanParseException> { block() } as? IbanParseException.Malformed)?.kind
    }
}
