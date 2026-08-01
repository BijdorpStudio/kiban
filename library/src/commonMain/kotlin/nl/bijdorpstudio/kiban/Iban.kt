/*
   Copyright 2023 Barend Garvelink, Eugen Martynov

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

import kotlin.Result.Companion.failure
import nl.bijdorpstudio.kiban.Iban.Companion.parse
import nl.bijdorpstudio.kiban.IbanParseException.Malformed.Kind

/**
 * An immutable value type representing an International Bank Account Number. Instances of this class have correct
 * check digits and a valid length for their country code. No country-specific validation is performed, other than
 * matching the length of the IBAN to its country code. Unknown country codes are not supported.
 *
 * @author Barend Garvelink https://github.com/barend
 *
 * Instances can only be obtained through [Iban.parse] or [Iban.compose], which validate the input and report failures
 * as a [Result] carrying an [IbanParseException]. Construction itself never fails.
 *
 * @property isInSwiftRegistry whether or not this IBAN data is from the SWIFT IBAN Registry.
 * @property isSEPA whether or not this IBAN is of a SEPA participating country.
 * @property plain the IBAN value, without any white space.
 * @property pretty the IBAN value, with spaces every four characters.
 * @since 1.0.0
 *
 * @see <a href="https://en.wikipedia.org/wiki/International_Bank_Account_Number">Wikipedia: International Bank Account Number</a>
 */
class Iban internal constructor(internal val value: String) : Comparable<Iban> {
    /**
     * Whether or not this IBAN data is from the SWIFT IBAN Registry.
     *
     * @return true if from SWIFT IBAN Registry, false otherwise.
     */
    val isInSwiftRegistry: Boolean

    /**
     * Whether or not this IBAN is of a SEPA participating country.
     *
     * @return true this IBAN is of a SEPA participating country, false otherwise.
     */
    val isSEPA: Boolean

    /**
     * Pretty-printed value, lazily initialized.
     */
    val pretty: String by lazy(LazyThreadSafetyMode.NONE) { addSpaces(value) }

    /**
     * Initializing constructor. Validation happens in [parse], so this constructor cannot fail.
     * @param value the IBAN value, without any white space, already validated by [parse].
     */
    init {
        val countryCode: String = value.substring(0, 2)
        this.isInSwiftRegistry = CountryCodes.isInSwiftRegistry(countryCode)
        this.isSEPA = CountryCodes.isSEPACountry(countryCode)
    }

    val countryCode: String
        /**
         * Returns the Country Code embedded in the IBAN.
         * @return the two-letter country code.
         */
        get() = value.substring(0, 2)

    /**
     * Returns the check digits of the IBAN.
     * @return the two check digits.
     */
    val checkDigits: String
        get() = value.substring(2, 4)

    /**
     * Returns the bank identifier embedded in the IBAN, if available.
     * @return the bank ID, or `null` if unknown for this country code.
     */
    val bankIdentifier: String?
        get() = CountryCodes.getBankIdentifierInternal(this)

    /**
     * Returns the branch identifier embedded in the IBAN, if available.
     * @return the branch ID, or `null` if unknown for this country code.
     */
    val branchIdentifier: String?
        get() = CountryCodes.getBranchIdentifierInternal(this)

    /**
     * Returns the IBAN without formatting.
     * @return the unformatted IBAN number.
     */
    val plain: String get() = value

    /**
     * Returns the IBAN without formatting.
     * @return the unformatted IBAN number.
     */
    @Deprecated("Use plain property instead", ReplaceWith("plain"))
    fun toPlainString(): String = plain

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Iban) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    /**
     * Returns the IBAN in standard formatting, with a space every four characters.
     * @return the formatted IBAN number.
     * @see [plain]
     */
    override fun toString(): String = pretty

    override fun compareTo(other: Iban): Int = value.compareTo(other.value)

    companion object {

        /**
         * Parses the given string into an IBAN object and confirms the check digits.
         * @param input the input, which can be either plain ("CC11ABCD123...") or formatted with (ASCII 0x20) space characters ("CC11 ABCD 123. ..").
         * @return a [Result] holding the parsed and validated IBAN object, or an [IbanParseException] describing why the input was rejected.
         * @see [parse]
         */
        operator fun invoke(input: CharSequence): Result<Iban> = parse(input)

        /**
         * The technically shortest possible IBAN. See [CountryCodes.SHORTEST_IBAN_LENGTH] for the shortest valid length.
         */
        const val SHORTEST_POSSIBLE_IBAN: Int = 5

        /**
         * Parses the given string into an IBAN object and confirms the check digits.
         *
         * This is the primary entry point of the library. It never throws for invalid user input; failures are returned
         * as a [Result.failure] carrying an [IbanParseException]. Use [Result.getOrThrow] for the semantics of the
         * deprecated throwing API.
         *
         * @param input the input, which can be either plain ("CC11ABCD123...") or formatted with (ASCII 0x20) space characters ("CC11 ABCD 123. ..").
         * @return a [Result] holding the parsed and validated IBAN object, or an [IbanParseException] describing why the input was rejected.
         * @see [IbanParseException]
         */
        fun parse(input: CharSequence): Result<Iban> =
            when (val rejection = validate(input)) {
                null -> Result.success(Iban(toPlain(input)))
                else -> failure(rejection.toException())
            }

        /**
         * Validates the given input without constructing an [IbanParseException], so that [String.isValidIban] and
         * [String.toIbanOrNull] can reject invalid input without paying for a captured stack trace. [parse] builds on
         * this and constructs the exception only when a caller actually needs one.
         *
         * @param input the input, which can be either plain ("CC11ABCD123...") or formatted with (ASCII 0x20) space characters ("CC11 ABCD 123. ..").
         * @return `null` if the input is a valid IBAN, or a [Rejection] describing why it was rejected.
         */
        internal fun validate(input: CharSequence): Rejection? {
            if (input.isEmpty()) {
                return Rejection.Malformed("", Kind.EMPTY)
            }
            if (!input.first().isLetterOrDigit() || !input.last().isLetterOrDigit()) {
                return Rejection.Malformed(
                    toPlain(input),
                    Kind.INVALID_BOUNDARY_CHARACTER,
                    "Input begins or ends in an invalid character: $input"
                )
            }
            val value = toPlain(input)
            if (value.length < SHORTEST_POSSIBLE_IBAN) {
                return Rejection.Malformed(value, Kind.TOO_SHORT)
            }
            if (!(value[2].isDigit() && value[3].isDigit())) {
                return Rejection.Malformed(value, Kind.NON_NUMERIC_CHECK_DIGITS)
            }
            val countryCode: String = value.substring(0, 2)
            val expectedLength: Int = CountryCodes.getLength(countryCode)
                ?: return Rejection.UnknownCountryCode(value, countryCode)
            if (expectedLength != value.length) {
                return Rejection.WrongLength(value, expectedLength, value.length)
            }
            val calculatedChecksum: Int = try {
                Modulo97.checksum(value)
            } catch (e: IllegalArgumentException) {
                return Rejection.Malformed(
                    value,
                    Kind.INVALID_CHARACTER,
                    e.message ?: "Invalid character in $value"
                )
            }
            if (calculatedChecksum != 1) {
                return Rejection.WrongChecksum(value)
            }
            return null
        }

        /**
         * Parses the given string into an IBAN object and confirms the check digits, throwing on invalid input.
         * @param input the input, which can be either plain ("CC11ABCD123...") or formatted ("CC11 ABCD 123. ..").
         * @return the parsed and validated IBAN object.
         * @throws [IllegalArgumentException] if the input is in some way invalid.
         * @see [parse]
         */
        @Deprecated("Use parse() instead", ReplaceWith("parse(input).getOrThrow()"))
        fun valueOf(input: CharSequence): Iban = parse(input).getOrThrow()

        /**
         * Composes an IBAN from the given country code and basic bank account number, calculating the check digits.
         * @param countryCode the country code.
         * @param bban the BBAN.
         * @return a [Result] holding the IBAN object composed of the given parts, or an [IbanParseException] describing why the parts were rejected.
         */
        fun compose(countryCode: CharSequence, bban: CharSequence): Result<Iban> {
            val sb = StringBuilder(CountryCodes.LONGEST_IBAN_LENGTH).append(countryCode).append("00").append(bban)
            val checkDigits = try {
                Modulo97.calculateCheckDigits(sb)
            } catch (e: IllegalArgumentException) {
                return failure(
                    IbanParseException.Malformed(
                        toPlain(sb),
                        Kind.INVALID_STRUCTURE,
                        e.message ?: "Cannot calculate check digits for $sb"
                    )
                )
            }
            sb.setRange(2, 4, checkDigits.toString().padStart(2, '0'))
            return parse(sb)
        }

        /**
         * Removes any spaces contained in the String thereby converting the input into a plain IBAN
         *
         * @param input possibly pretty printed IBAN
         * @return plain IBAN
         */
        fun toPlain(input: CharSequence): String =
            input
                .filter { !it.isWhitespace() }
                .toString()

        /**
         * Ensures that the input is pretty printed by first removing any spaces the String might contain and then adding spaces in the right places.
         *
         * This can be useful when prompting a user to correct wrong input
         *
         * @param input
         * plain or pretty printed IBAN
         * @return pretty printed IBAN
         */
        fun format(input: CharSequence): String = addSpaces(toPlain(input))

        /**
         * Ensures that the input is pretty printed by first removing any spaces the String might contain and then adding spaces in the right places.
         *
         * This can be useful when prompting a user to correct wrong input
         *
         * @param input
         * plain or pretty printed IBAN
         * @return pretty printed IBAN
         */
        @Deprecated("Use format() instead", ReplaceWith("format(input)"))
        fun toPretty(input: CharSequence): String = format(input)

        /**
         * Converts a plain to a pretty printed IBAN
         *
         * @param value
         * plain iban
         * @return pretty printed IBAN
         */
        private fun addSpaces(value: CharSequence): String =
            value
                .chunked(4)
                .joinToString(" ")
    }
}

/**
 * Parses the given string into an IBAN object and confirms the check digits.
 * @return a [Result] holding the parsed and validated IBAN object, or an [IbanParseException] describing why the input was rejected.
 * @see Iban.parse
 */
fun String.toIban(): Result<Iban> = Iban.parse(this)

/**
 * Parses the given string into an IBAN object and confirms the check digits, discarding the failure detail.
 * @return the parsed and validated IBAN object, or `null` if the input is in some way invalid.
 * @see Iban.parse
 */
fun String.toIbanOrNull(): Iban? = if (Iban.validate(this) == null) Iban(Iban.toPlain(this)) else null

/**
 * Returns whether the given string is a valid IBAN.
 */
fun String.isValidIban(): Boolean = Iban.validate(this) == null
