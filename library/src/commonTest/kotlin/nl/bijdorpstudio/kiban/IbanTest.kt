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

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import de.infix.testBalloon.framework.core.testSuite

internal const val VALID_IBAN = "NL03ABNA0143267469"
internal const val INVALID_IBAN = "NL13ABNA0143267469"

/**
 * One input per rejection kind the parser distinguishes, plus the valid IBAN. Shared by the tests
 * that assert every entry point agrees on accept/reject and that nothing but [IbanParseException]
 * escapes.
 */
private val rejectionKindInputs =
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

/** Miscellaneous tests for the [Iban] class. */
val IbanTest by testSuite {
    test("Operator invoke should parse IBAN") {
        // Called through the companion explicitly: inside the library module the private
        // constructor would win over invoke for an exact String argument.
        val iban = Iban.Companion.invoke(VALID_IBAN)
        assertThat(iban.plain).isEqualTo(VALID_IBAN)
    }

    test("Operator invoke should throw for invalid input") {
        assertFailure { Iban(INVALID_IBAN) }.isInstanceOf<IbanParseException.WrongChecksum>()
    }

    test("toIban extension should parse IBAN") {
        val iban = VALID_IBAN.toIban()
        assertThat(iban.plain).isEqualTo(VALID_IBAN)
    }

    test("toIban extension should throw for invalid IBAN") {
        assertFailure { INVALID_IBAN.toIban() }.isInstanceOf<IbanParseException.WrongChecksum>()
    }

    test("toIbanOrNull extension should parse IBAN") {
        assertThat(VALID_IBAN.toIbanOrNull()?.plain).isEqualTo(VALID_IBAN)
    }

    test("toIbanOrNull extension should return null for invalid IBAN") {
        assertThat(INVALID_IBAN.toIbanOrNull()).isNull()
    }

    test("isValidIban extension should return true for valid IBAN") {
        assertThat(VALID_IBAN.isValidIban()).isTrue()
    }

    test("isValidIban extension should return false for invalid IBAN") {
        assertThat(INVALID_IBAN.isValidIban()).isFalse()
    }

    for (input in rejectionKindInputs + VALID_IBAN) {
        test("isValidIban and toIbanOrNull should agree with invoke for '$input'") {
            val expectedSuccess = runCatching { Iban(input) }.isSuccess

            assertThat(input.isValidIban(), "isValidIban for '$input'").isEqualTo(expectedSuccess)
            assertThat(input.toIbanOrNull() != null, "toIbanOrNull for '$input'")
                .isEqualTo(expectedSuccess)
        }
    }

    test("Valid IBAN should return country code") {
        assertThat(Iban(VALID_IBAN).countryCode).isEqualTo("NL")
    }

    test("Valid IBAN should return check digits") {
        assertThat(Iban(VALID_IBAN).checkDigits).isEqualTo("03")
    }

    test("Invoke should accept toString output of valid IBAN") {
        val original = Iban(VALID_IBAN)
        val copy = Iban(original.toString())
        assertThat(copy).isEqualTo(original)
    }

    test("Invoke should reject empty input") {
        assertFailure { Iban("") }
            .isInstanceOf<IbanParseException.Malformed>()
            .prop(IbanParseException.Malformed::kind)
            .isEqualTo(IbanParseException.Malformed.Kind.EMPTY)
    }

    test("Invoke should reject invalid input") {
        assertFailure { Iban("Shenanigans!") }
            .isInstanceOf<IbanParseException.Malformed>()
            .all {
                prop(IbanParseException.Malformed::kind)
                    .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
                hasMessage("Input begins or ends in an invalid character: Shenanigans!")
            }
    }

    test("Invoke should reject leading whitespace") {
        assertFailure { Iban(" $VALID_IBAN") }
            .isInstanceOf<IbanParseException.Malformed>()
            .all {
                prop(IbanParseException.Malformed::kind)
                    .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
                hasMessage("Input begins or ends in an invalid character:  $VALID_IBAN")
            }
    }

    test("Invoke should reject trailing whitespace") {
        assertFailure { Iban("$VALID_IBAN ") }
            .isInstanceOf<IbanParseException.Malformed>()
            .all {
                prop(IbanParseException.Malformed::kind)
                    .isEqualTo(IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER)
                hasMessage("Input begins or ends in an invalid character: $VALID_IBAN ")
            }
    }

    test("Invoke should reject too short input") {
        assertFailure { Iban("NL03") }
            .isInstanceOf<IbanParseException.Malformed>()
            .all {
                prop(IbanParseException.Malformed::kind)
                    .isEqualTo(IbanParseException.Malformed.Kind.TOO_SHORT)
                hasMessage("Length is too short to be an IBAN: NL03")
            }
    }

    test("Invoke should reject non numeric check digits") {
        assertFailure { Iban("NLAB0143267469") }
            .isInstanceOf<IbanParseException.Malformed>()
            .all {
                prop(IbanParseException.Malformed::kind)
                    .isEqualTo(IbanParseException.Malformed.Kind.NON_NUMERIC_CHECK_DIGITS)
                hasMessage("Characters at index 2 and 3 not both numeric. NLAB0143267469")
            }
    }

    test("Invoke should reject unsupported characters") {
        val invalidCharacter = VALID_IBAN.replaceRange(6, 7, "_")

        assertFailure { Iban(invalidCharacter) }
            .isInstanceOf<IbanParseException.Malformed>()
            .prop(IbanParseException.Malformed::kind)
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_CHARACTER)
    }

    test("Invoke should reject unknown country code") {
        assertFailure { Iban("UU345678345543234") }
            .isInstanceOf<IbanParseException.UnknownCountryCode>()
            .all {
                prop(IbanParseException.UnknownCountryCode::countryCode).isEqualTo("UU")
                hasMessage("Unknown country code: UU")
            }
    }

    test("Invoke should reject wrong length") {
        val tooLong = VALID_IBAN + "0"

        assertFailure { Iban(tooLong) }
            .isInstanceOf<IbanParseException.WrongLength>()
            .prop(IbanParseException.WrongLength::expectedLength)
            .isEqualTo(18)
    }

    test("Invoke should reject checksum failure") {
        assertFailure { Iban(INVALID_IBAN) }
            .isInstanceOf<IbanParseException.WrongChecksum>()
            .all {
                prop(IbanParseException.WrongChecksum::input).isEqualTo(INVALID_IBAN)
                hasMessage("Wrong check sum for $INVALID_IBAN")
            }
    }

    test("Invoke failure should be an IllegalArgumentException") {
        assertFailure { Iban(INVALID_IBAN) }.isInstanceOf<IllegalArgumentException>()
    }

    test("Compose should handle correct input") {
        val composed =
            Iban.compose(
                countryCode = VALID_IBAN.substring(0, 2),
                bban = VALID_IBAN.substring(4),
            )
        assertThat(composed).isEqualTo(Iban(VALID_IBAN))
    }

    test("Compose should handle check digits of ten and higher") {
        val composed = Iban.compose(countryCode = "BI", bban = "10000100010000332045181")

        assertThat(composed.plain).isEqualTo("BI4210000100010000332045181")
    }

    test("Compose should reject blank country code") {
        assertFailure { Iban.compose(countryCode = "  ", bban = VALID_IBAN.substring(4)) }
            .isInstanceOf<IbanParseException>()
    }

    test("Compose should reject malformed country code") {
        assertFailure { Iban.compose(countryCode = "potato", bban = VALID_IBAN.substring(4)) }
            .isInstanceOf<IbanParseException.Malformed>()
            .prop(IbanParseException.Malformed::kind)
            .isEqualTo(IbanParseException.Malformed.Kind.INVALID_STRUCTURE)
    }

    test("Compose should reject unknown country code") {
        assertFailure { Iban.compose(countryCode = "XX", bban = VALID_IBAN.substring(4)) }
            .isInstanceOf<IbanParseException.UnknownCountryCode>()
    }

    test("Compose should reject wrong length BBAN") {
        assertFailure {
            Iban.compose(
                countryCode = VALID_IBAN.substring(0, 2),
                bban = VALID_IBAN.substring(5),
            )
        }
            .isInstanceOf<IbanParseException.WrongLength>()
    }

    test("Equals contract should be satisfied") {
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

    for ((input, formatted) in
        listOf(
            "" to "",
            "12" to "12",
            "1 2" to "12",
            "1234" to "1234",
            "1 2 3 4" to "1234",
            "12345" to "1234 5",
            "1234 5" to "1234 5",
            "12345678" to "1234 5678",
            "1234 5678" to "1234 5678",
            "123456789" to "1234 5678 9",
            "1234 5678 9" to "1234 5678 9",
        )) {
        test("Add spaces should format '$input' as '$formatted'") {
            assertThat(Iban.addSpaces(Iban.toPlain(input))).isEqualTo(formatted)
        }
    }

    for ((input, plain) in
        listOf(
            "" to "",
            "12" to "12",
            "1 2" to "12",
            "1234" to "1234",
            "1 2 3 4" to "1234",
            "12345" to "12345",
            "1234 5" to "12345",
            "12345678" to "12345678",
            "1234 5678" to "12345678",
            "123456789" to "123456789",
            "1234 5678 9" to "123456789",
        )) {
        test("To plain should reduce '$input' to '$plain'") {
            assertThat(Iban.toPlain(input)).isEqualTo(plain)
        }
    }

    test("Lexical sort should order IBANs correctly") {
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
    for (input in rejectionKindInputs) {
        test("Only IbanParseException escapes invoke and toIban for '$input'") {
            assertFailure { Iban(input) }.isInstanceOf<IbanParseException>()
            assertFailure { input.toIban() }.isInstanceOf<IbanParseException>()
        }
    }

    test("Only IbanParseException escapes compose for every rejection kind") {
        assertFailure { Iban.compose("  ", VALID_IBAN.substring(4)) }
            .isInstanceOf<IbanParseException>()
        assertFailure { Iban.compose("potato", VALID_IBAN.substring(4)) }
            .isInstanceOf<IbanParseException>()
        assertFailure { Iban.compose("XX", VALID_IBAN.substring(4)) }
            .isInstanceOf<IbanParseException>()
        assertFailure { Iban.compose(VALID_IBAN.substring(0, 2), VALID_IBAN.substring(5)) }
            .isInstanceOf<IbanParseException>()
    }
}
