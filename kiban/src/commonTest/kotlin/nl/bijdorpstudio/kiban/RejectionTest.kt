/*
  Copyright 2026 Barend Garvelink, Eugen Martynov

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
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import de.infix.testBalloon.framework.core.testSuite
import nl.bijdorpstudio.kiban.IbanParseException.Malformed.Kind

/**
 * Every kind paired with the message the exception built from it carries. Exhaustive by
 * construction: the guard test at the bottom fails if a kind is added without an entry here.
 */
private val messages: List<Pair<Kind, String>> =
    listOf(
        Kind.Empty to "Input is empty",
        Kind.TooShort to "Length is too short to be an IBAN: $VALID_IBAN",
        Kind.NonNumericCheckDigits to "Characters at index 2 and 3 not both numeric. $VALID_IBAN",
        Kind.NonUpperCaseCountryCode to "Country code is not upper case: NL. $VALID_IBAN",
        Kind.InvalidBoundaryCharacter(' ', atStart = true) to
            "Input begins with an invalid character ' ': $VALID_IBAN",
        Kind.InvalidBoundaryCharacter('!', atStart = false) to
            "Input ends with an invalid character '!': $VALID_IBAN",
        Kind.InvalidCharacter('_', 6) to "Invalid character '_' at index 6 in $VALID_IBAN",
        Kind.InvalidStructure("Country code should be length 2 but was XYZ") to
            "Country code should be length 2 but was XYZ",
    )

/** Tests for the internal [Rejection] type behind the non-throwing validation paths. */
val RejectionTest by testSuite {
    for ((kind, expectedMessage) in messages) {
        test("Malformed rejection of $kind describes itself") {
            val exception = Rejection.Malformed(VALID_IBAN, kind).toException()

            assertThat(exception)
                .isInstanceOf<IbanParseException.Malformed>()
                .hasMessage(expectedMessage)
        }

        test("Malformed rejection of $kind carries its kind into the exception unchanged") {
            val exception = Rejection.Malformed(VALID_IBAN, kind).toException()

            assertThat(exception)
                .isInstanceOf<IbanParseException.Malformed>()
                .prop(IbanParseException.Malformed::kind)
                .isEqualTo(kind)
        }
    }

    test("Every malformed kind has a message of its own") {
        // A sealed hierarchy has no `entries`, so exhaustiveness is checked the way the compiler
        // checks it: this `when` stops compiling when a kind is added, and the branch added to
        // fix it has to name a kind that `messages` covers.
        val covered =
            messages
                .map { (kind, _) ->
                    when (kind) {
                        Kind.Empty -> "Empty"
                        Kind.TooShort -> "TooShort"
                        Kind.NonNumericCheckDigits -> "NonNumericCheckDigits"
                        Kind.NonUpperCaseCountryCode -> "NonUpperCaseCountryCode"
                        is Kind.InvalidBoundaryCharacter -> "InvalidBoundaryCharacter"
                        is Kind.InvalidCharacter -> "InvalidCharacter"
                        is Kind.InvalidStructure -> "InvalidStructure"
                    }
                }
                .toSet()

        assertThat(covered.size).isEqualTo(7)
    }
}
