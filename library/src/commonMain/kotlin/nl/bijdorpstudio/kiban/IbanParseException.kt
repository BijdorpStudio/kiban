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

/**
 * Thrown by a failed [Iban.invoke] (`Iban(input)`), [Iban.compose] or [String.toIban] to describe
 * why the input was rejected. Extending [IllegalArgumentException] means callers who only care that
 * parsing failed don't need to catch this type specifically.
 *
 * Instances are constructed by kiban only: the subtypes exist to be caught and inspected, not to be
 * created. Their constructors are internal, so the message wording of a rejection stays the
 * library's to decide.
 *
 * @property input the offending input, with the (ASCII 0x20) spaces that group a pretty-printed
 *   IBAN removed. Nothing else is stripped: any other whitespace the input carried is preserved
 *   here, because it is a character an IBAN cannot contain and is often the very reason for the
 *   rejection.
 */
sealed class IbanParseException(
    val input: String,
    message: String,
) : IllegalArgumentException(message) {

    /**
     * The input is not shaped like an IBAN at all: it is empty, too short, contains unsupported
     * characters or does not carry numeric check digits.
     *
     * @property kind the specific structural problem detected.
     */
    class Malformed
    internal constructor(
        input: String,
        val kind: Kind,
        message: String,
    ) : IbanParseException(input, message) {

        /** The structural problem that made the input malformed. */
        enum class Kind {
            /** The input is empty. */
            EMPTY,

            /** The input begins or ends in a character that cannot occur in an IBAN. */
            INVALID_BOUNDARY_CHARACTER,

            /** The input is shorter than [Iban.SHORTEST_POSSIBLE_IBAN_LENGTH]. */
            TOO_SHORT,

            /** The characters at index 2 and 3 are not both ASCII digits. */
            NON_NUMERIC_CHECK_DIGITS,

            /**
             * The country code is a known IBAN country code, but is not written in upper case.
             * IBANs are upper case by definition, and kiban rejects rather than normalizes.
             */
            NON_UPPER_CASE_COUNTRY_CODE,

            /** The input contains a character outside the ASCII range `[A-Za-z0-9 ]`. */
            INVALID_CHARACTER,

            /**
             * The parts handed to [Iban.compose] cannot be assembled into an IBAN, because they are
             * too short, carry unsupported characters, or because the country code is not exactly
             * two characters.
             */
            INVALID_STRUCTURE,
        }
    }

    /**
     * The country code of the input is not a known IBAN country code.
     *
     * @property countryCode the two-letter country code that was not recognized.
     */
    class UnknownCountryCode
    internal constructor(
        input: String,
        val countryCode: String,
    ) : IbanParseException(input, "Unknown country code: $countryCode")

    /**
     * The input has a valid country code, but its length does not match the length registered for
     * that country.
     *
     * @property expectedLength the length registered for the country code of the input.
     * @property actualLength the length of the input.
     */
    class WrongLength
    internal constructor(
        input: String,
        val expectedLength: Int,
        val actualLength: Int,
    ) : IbanParseException(input, "Wrong length $actualLength for $input expected: $expectedLength")

    /** The input is structurally valid, but fails MOD-97 check digit verification. */
    class WrongChecksum internal constructor(input: String) :
        IbanParseException(input, "Wrong check sum for $input")
}

/**
 * A lightweight, non-throwing description of why an input was rejected by [Iban.validate].
 *
 * Carries the same information as [IbanParseException] without paying for a captured stack trace,
 * so that the non-throwing validation paths ([String.isValidIban] and [String.toIbanOrNull]) can
 * reject input without allocating a [Throwable]. [toException] builds the actual
 * [IbanParseException] for callers that need it, such as [Iban.invoke].
 */
internal sealed class Rejection(val input: String) {

    class Malformed(
        input: String,
        val kind: IbanParseException.Malformed.Kind,
        val detail: String? = null,
    ) : Rejection(input)

    class UnknownCountryCode(input: String, val countryCode: String) : Rejection(input)

    class WrongLength(input: String, val expectedLength: Int, val actualLength: Int) :
        Rejection(input)

    class WrongChecksum(input: String) : Rejection(input)

    fun toException(): IbanParseException =
        when (this) {
            is Malformed ->
                IbanParseException.Malformed(input, kind, detail ?: defaultMessage(kind))
            is UnknownCountryCode -> IbanParseException.UnknownCountryCode(input, countryCode)
            is WrongLength -> IbanParseException.WrongLength(input, expectedLength, actualLength)
            is WrongChecksum -> IbanParseException.WrongChecksum(input)
        }

    private fun defaultMessage(kind: IbanParseException.Malformed.Kind): String =
        when (kind) {
            IbanParseException.Malformed.Kind.EMPTY -> "Input is empty"
            IbanParseException.Malformed.Kind.TOO_SHORT ->
                "Length is too short to be an IBAN: $input"
            IbanParseException.Malformed.Kind.NON_NUMERIC_CHECK_DIGITS ->
                "Characters at index 2 and 3 not both numeric. $input"
            IbanParseException.Malformed.Kind.NON_UPPER_CASE_COUNTRY_CODE ->
                "Country code is not upper case: ${input.take(2)}. $input"
            IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER,
            IbanParseException.Malformed.Kind.INVALID_CHARACTER,
            IbanParseException.Malformed.Kind.INVALID_STRUCTURE ->
                error("$kind rejections must supply a detail message")
        }
}
