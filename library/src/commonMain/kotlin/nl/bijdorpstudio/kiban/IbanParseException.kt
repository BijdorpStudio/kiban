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
 * The failure carried by the [Result] of a failed [Iban.parse] or [Iban.compose].
 *
 * Instances are never thrown by the library itself; they are wrapped in [Result.failure] so that callers can decide
 * whether to inspect the failure or to rethrow it with [Result.getOrThrow]. Extending [IllegalArgumentException] keeps
 * the semantics of the deprecated throwing API identical.
 *
 * @property input the offending input, with any white space removed.
 */
sealed class IbanParseException(
    val input: String,
    message: String
) : IllegalArgumentException(message) {

    /**
     * The input is not shaped like an IBAN at all: it is empty, too short, contains unsupported characters or does not
     * carry numeric check digits.
     *
     * @property kind the specific structural problem detected.
     */
    class Malformed(
        input: String,
        val kind: Kind,
        message: String
    ) : IbanParseException(input, message) {

        /**
         * The structural problem that made the input malformed.
         */
        enum class Kind {
            /** The input is empty. */
            EMPTY,

            /** The input begins or ends in a character that cannot occur in an IBAN. */
            INVALID_BOUNDARY_CHARACTER,

            /** The input is shorter than [Iban.SHORTEST_POSSIBLE_IBAN]. */
            TOO_SHORT,

            /** The characters at index 2 and 3 are not both numeric. */
            NON_NUMERIC_CHECK_DIGITS,

            /** The input contains a character outside the range `[A-Za-z0-9 ]`. */
            INVALID_CHARACTER,

            /**
             * The parts handed to [Iban.compose] cannot be assembled into an IBAN, because they are too short, carry
             * unsupported characters, or because the country code is not exactly two characters.
             */
            INVALID_STRUCTURE
        }
    }

    /**
     * The country code of the input is not a known IBAN country code.
     *
     * @property countryCode the two-letter country code that was not recognized.
     */
    class UnknownCountryCode(
        input: String,
        val countryCode: String
    ) : IbanParseException(input, "Unknown country code: $countryCode")

    /**
     * The input has a valid country code, but its length does not match the length registered for that country.
     *
     * @property expectedLength the length registered for the country code of the input.
     * @property actualLength the length of the input.
     */
    class WrongLength(
        input: String,
        val expectedLength: Int,
        val actualLength: Int
    ) : IbanParseException(input, "Wrong length $actualLength for $input expected: $expectedLength")

    /**
     * The input is structurally valid, but fails MOD-97 check digit verification.
     */
    class WrongChecksum(
        input: String
    ) : IbanParseException(input, "Wrong check sum for $input")
}

/**
 * A lightweight, non-throwing description of why an input was rejected by [Iban.validate].
 *
 * Carries the same information as [IbanParseException] without paying for a captured stack trace, so that the
 * non-throwing validation paths ([String.isValidIban] and [String.toIbanOrNull]) can reject input without allocating
 * a [Throwable]. [toException] builds the actual [IbanParseException] for callers that need it, such as [Iban.parse].
 */
internal sealed class Rejection(val input: String) {

    class Malformed(
        input: String,
        val kind: IbanParseException.Malformed.Kind,
        val detail: String? = null
    ) : Rejection(input)

    class UnknownCountryCode(input: String, val countryCode: String) : Rejection(input)

    class WrongLength(input: String, val expectedLength: Int, val actualLength: Int) : Rejection(input)

    class WrongChecksum(input: String) : Rejection(input)

    fun toException(): IbanParseException = when (this) {
        is Malformed -> IbanParseException.Malformed(input, kind, detail ?: defaultMessage(kind))
        is UnknownCountryCode -> IbanParseException.UnknownCountryCode(input, countryCode)
        is WrongLength -> IbanParseException.WrongLength(input, expectedLength, actualLength)
        is WrongChecksum -> IbanParseException.WrongChecksum(input)
    }

    private fun defaultMessage(kind: IbanParseException.Malformed.Kind): String = when (kind) {
        IbanParseException.Malformed.Kind.EMPTY -> "Input is empty"
        IbanParseException.Malformed.Kind.TOO_SHORT -> "Length is too short to be an IBAN: $input"
        IbanParseException.Malformed.Kind.NON_NUMERIC_CHECK_DIGITS ->
            "Characters at index 2 and 3 not both numeric. $input"
        IbanParseException.Malformed.Kind.INVALID_BOUNDARY_CHARACTER,
        IbanParseException.Malformed.Kind.INVALID_CHARACTER,
        IbanParseException.Malformed.Kind.INVALID_STRUCTURE ->
            error("$kind rejections must supply a detail message")
    }
}
