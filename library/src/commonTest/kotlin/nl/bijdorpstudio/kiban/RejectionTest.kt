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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import de.infix.testBalloon.framework.core.testSuite
import nl.bijdorpstudio.kiban.IbanParseException.Malformed.Kind

/** The default wording each remaining kind carries when no detail is supplied. */
private val DEFAULT_MESSAGES =
    mapOf(
        Kind.EMPTY to "Input is empty",
        Kind.TOO_SHORT to "Length is too short to be an IBAN: $VALID_IBAN",
        Kind.NON_NUMERIC_CHECK_DIGITS to
            "Characters at index 2 and 3 not both numeric. $VALID_IBAN",
        Kind.NON_UPPER_CASE_COUNTRY_CODE to
            "Country code is not upper case: ${VALID_IBAN.take(2)}. $VALID_IBAN",
    )

/** The kinds that carry no default wording and so have to be given a detail message. */
private val KINDS_REQUIRING_DETAIL =
    listOf(Kind.INVALID_BOUNDARY_CHARACTER, Kind.INVALID_CHARACTER, Kind.INVALID_STRUCTURE)

/** Tests for the internal [Rejection] type behind the non-throwing validation paths. */
val RejectionTest by testSuite {
    // The detail requirement is an invariant of the rejection, not of the exception built from
    // it: a rejection that breaks it has to fail where it is constructed, not later on a
    // materialization path that isValidIban and toIbanOrNull never take.
    for (kind in KINDS_REQUIRING_DETAIL) {
        test("Malformed rejection of kind $kind without a detail fails at construction") {
            assertFailure { Rejection.Malformed(VALID_IBAN, kind) }
                .isInstanceOf<IllegalArgumentException>()
                .messageContains("must supply a detail message")
        }

        test("Malformed rejection of kind $kind carries its detail into the exception") {
            val rejection = Rejection.Malformed(VALID_IBAN, kind, "Something specific")

            assertThat(rejection.toException()).hasMessage("Something specific")
        }
    }

    for ((kind, expectedMessage) in DEFAULT_MESSAGES) {
        test("Malformed rejection of kind $kind builds its default message without a detail") {
            val exception = Rejection.Malformed(VALID_IBAN, kind).toException()

            assertThat(exception).isInstanceOf<IbanParseException.Malformed>()
            assertThat((exception as IbanParseException.Malformed).kind).isEqualTo(kind)
            assertThat(exception).hasMessage(expectedMessage)
        }

        test("Malformed rejection of kind $kind prefers an explicit detail over the default") {
            val rejection = Rejection.Malformed(VALID_IBAN, kind, "Explicit detail")

            assertThat(rejection.toException()).hasMessage("Explicit detail")
        }
    }

    test("Every malformed kind is covered by exactly one of the two groups") {
        assertThat(KINDS_REQUIRING_DETAIL + DEFAULT_MESSAGES.keys)
            .containsExactlyInAnyOrder(*Kind.entries.toTypedArray())
    }
}
