/*
   Original copyright 2023 Barend Garvelink

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

import nl.bijdorpstudio.kiban.Iban.Companion.parse
import nl.bijdorpstudio.kiban.Iban.Companion.valueOf

/**
 * An immutable value type representing an International Bank Account Number. Instances of this class have correct
 * check digits and a valid length for their country code. No country-specific validation is performed, other than
 * matching the length of the IBAN to its country code. Unknown country codes are not supported.
 *
 * @author Barend Garvelink https://github.com/barend
 *
 * @property value the IBAN value, without any white space.
 * @property isInSwiftRegistry whether or not this IBAN data is from the SWIFT IBAN Registry.
 * @property isSEPA whether or not this IBAN is of a SEPA participating country.
 * @property valuePretty the IBAN value, with spaces every four characters.
 * @throws [IllegalArgumentException] if the input is null, malformed or otherwise fails validation.
 * @since 1.0.0
 *
 * @see <a href="https://en.wikipedia.org/wiki/International_Bank_Account_Number">Wikipedia: International Bank Account Number</a>
 */
class Iban private constructor(internal val value: String) : Comparable<Iban> {
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
    private val valuePretty: String by lazy(LazyThreadSafetyMode.NONE) { addSpaces(value) }

    /**
     * Initializing constructor.
     * @param value the IBAN value, without any white space.
     * @throws [IllegalArgumentException] if the input is null, malformed or otherwise fails validation.
     */
    init {
        if (value.length < SHORTEST_POSSIBLE_IBAN) {
            throw IllegalArgumentException("Length is too short to be an IBAN: $value")
        }
        if (!(value[2].isDigit() && value[3].isDigit())) {
            throw IllegalArgumentException("Characters at index 2 and 3 not both numeric. $value")
        }
        val countryCode: String = value.substring(0, 2)
        val expectedLength: Int = CountryCodes.getLengthForCountryCode(countryCode)
        if (expectedLength < 0) {
            throw IllegalArgumentException("Unknown country code: $countryCode")
        }
        if (expectedLength != value.length) {
            throw IllegalArgumentException("Wrong length ${value.length} for $value expected: $expectedLength")
        }
        val calculatedChecksum: Int = Modulo97.checksum(value)
        if (calculatedChecksum != 1) {
            throw IllegalArgumentException("Wrong check sum for $value")
        }
        this.isInSwiftRegistry = CountryCodes.isInSwiftRegistry(countryCode)
        this.isSEPA = CountryCodes.isSEPACountry(countryCode)
    }

    val countryCode: String
        /**
         * Returns the Country Code embedded in the IBAN.
         * @return the two-letter country code.
         */
        get() = value.substring(0, 2)

    val checkDigits: String
        /**
         * Returns the check digits of the IBAN.
         * @return the two check digits.
         */
        get() = value.substring(2, 4)

    /**
     * Returns the IBAN without formatting.
     * @return the unformatted IBAN number.
     */
    fun toPlainString(): String = value

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
     * @see [toPlainString]
     */
    override fun toString(): String = valuePretty

    override fun compareTo(other: Iban): Int = value.compareTo(other.value)

    companion object {

        /**
         * The technically shortest possible IBAN. See [CountryCodes.SHORTEST_IBAN_LENGTH] for the shortest valid length.
         */
        const val SHORTEST_POSSIBLE_IBAN: Int = 5

        /**
         * Parses the given string into an IBAN object and confirms the check digits.
         * @param input the input, which can be either plain ("CC11ABCD123...") or formatted with (ASCII 0x20) space characters ("CC11 ABCD 123. ..").
         * @return the parsed and validated IBAN object
         * @throws [IllegalArgumentException] if the input is in some way invalid.
         * @see [valueOf]
         */
        fun parse(input: CharSequence): Iban {
            if (input.isEmpty()) {
                throw IllegalArgumentException("Input is empty")
            }
            if (!input.first().isLetterOrDigit() || !input.last().isLetterOrDigit()) {
                throw IllegalArgumentException("Input begins or ends in an invalid character: $input")
            }
            return Iban(toPlain(input))
        }

        /**
         * Parses the given string into an IBAN object and confirms the check digits, but returns null for null.
         * @param input the input, which can be either plain ("CC11ABCD123...") or formatted ("CC11 ABCD 123. ..").
         * @return the parsed and validated IBAN object, or null.
         * @throws [IllegalArgumentException] if the input is in some way invalid.
         * @see [parse]
         */
        fun valueOf(input: CharSequence): Iban? = parse(input)

        /**
         * Composes an IBAN from the given country code and basic bank account number.
         * @param countryCode the country code.
         * @param bban the BBAN.
         * @return an IBAN object composed from the given parts, if valid.
         * @throws [IllegalArgumentException] if the input is in some way invalid.
         */
        fun compose(countryCode: CharSequence, bban: CharSequence): Iban? {
            val sb = StringBuilder(CountryCodes.LONGEST_IBAN_LENGTH).append(countryCode).append("00").append(bban)
            val checkDigits = Modulo97.calculateCheckDigits(sb)
            if (checkDigits < 10) {
                sb[3] = ('0'.code + checkDigits).toChar()
            } else {
                sb.replaceRange(2..3, checkDigits.toString())
            }
            return parse(sb.toString())
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
        fun toPretty(input: CharSequence): String {
            return addSpaces(toPlain(input))
        }

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
