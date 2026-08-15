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

import nl.bijdorpstudio.kiban.IbanParseException.Malformed.Kind

/**
 * An immutable value type representing an International Bank Account Number. Instances of this
 * class have correct check digits and a valid length for their country code. No country-specific
 * validation is performed, other than matching the length of the IBAN to its country code. Unknown
 * country codes are not supported.
 *
 * Instances can only be obtained through [Iban.invoke] (`Iban(input)`) or [Iban.compose], which
 * validate the input and throw an [IbanParseException] on failure. Construction itself never fails.
 *
 * @property isInSwiftRegistry whether or not this IBAN data is from the SWIFT IBAN Registry.
 * @property isSEPA whether or not this IBAN is of a SEPA participating country.
 * @property plain the IBAN value, without any white space.
 * @property pretty the IBAN value, with spaces every four characters.
 * @see <a href="https://en.wikipedia.org/wiki/International_Bank_Account_Number">Wikipedia:
 *   International Bank Account Number</a>
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

    /** Pretty-printed value, lazily initialized. */
    val pretty: String by lazy(LazyThreadSafetyMode.NONE) { addSpaces(value) }

    /**
     * Initializing constructor. Validation happens before construction, so this constructor cannot
     * fail. the IBAN value, without any white space, already validated by the caller.
     */
    init {
        val countryCode: String = value.substring(0, 2)
        this.isInSwiftRegistry = CountryCodes.isInSwiftRegistry(countryCode)
        this.isSEPA = CountryCodes.isSEPACountry(countryCode)
    }

    val countryCode: String
        /**
         * Returns the Country Code embedded in the IBAN.
         *
         * @return the two-letter country code.
         */
        get() = value.substring(0, 2)

    /**
     * Returns the check digits of the IBAN.
     *
     * @return the two check digits.
     */
    val checkDigits: String
        get() = value.substring(2, 4)

    /**
     * Returns the bank identifier embedded in the IBAN, if available.
     *
     * @return the bank ID, or `null` if unknown for this country code.
     */
    val bankIdentifier: String?
        get() = CountryCodes.getBankIdentifier(this)

    /**
     * Returns the branch identifier embedded in the IBAN, if available.
     *
     * @return the branch ID, or `null` if unknown for this country code.
     */
    val branchIdentifier: String?
        get() = CountryCodes.getBranchIdentifier(this)

    /**
     * Returns the IBAN without formatting.
     *
     * @return the unformatted IBAN number.
     */
    val plain: String
        get() = value

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
     *
     * @return the formatted IBAN number.
     * @see [plain]
     */
    override fun toString(): String = pretty

    override fun compareTo(other: Iban): Int = value.compareTo(other.value)

    companion object {

        /**
         * Parses the given string into an IBAN object and confirms the check digits.
         *
         * Input must be ASCII: ISO 13616 defines the IBAN character set as `A-Z0-9`, so non-ASCII
         * look-alikes such as fullwidth digits are rejected rather than normalized. Normalize
         * (NFKC) user input before parsing if your input layer can produce them.
         *
         * @param input the input, which can be either plain ("CC11ABCD123...") or formatted with
         *   (ASCII 0x20) space characters ("CC11 ABCD 123. ..").
         * @return the parsed and validated IBAN object.
         * @throws IbanParseException describing why the input was rejected.
         */
        @Throws(IbanParseException::class)
        operator fun invoke(input: CharSequence): Iban =
            when (val rejection = validate(input)) {
                null -> Iban(toPlain(input))
                else -> throw rejection.toException()
            }

        /**
         * The technically shortest possible IBAN. See [CountryCodes.SHORTEST_IBAN_LENGTH] for the
         * shortest valid length.
         */
        const val SHORTEST_POSSIBLE_IBAN: Int = 5

        /**
         * Wraps an already-validated, whitespace-stripped IBAN string, without paying for
         * validation a second time.
         */
        internal fun ofValidated(plain: String): Iban = Iban(plain)

        /**
         * Validates the given input without constructing an [IbanParseException], so that
         * [String.isValidIban] and [String.toIbanOrNull] can reject invalid input without paying
         * for a captured stack trace. [invoke] builds on this and constructs the exception only
         * when a caller actually needs one.
         *
         * @param input the input, which can be either plain ("CC11ABCD123...") or formatted with
         *   (ASCII 0x20) space characters ("CC11 ABCD 123. ..").
         * @return `null` if the input is a valid IBAN, or a [Rejection] describing why it was
         *   rejected.
         */
        internal fun validate(input: CharSequence): Rejection? {
            if (input.isEmpty()) {
                return Rejection.Malformed("", Kind.EMPTY)
            }
            if (!input.first().isAsciiLetterOrDigit() || !input.last().isAsciiLetterOrDigit()) {
                return Rejection.Malformed(
                    toPlain(input),
                    Kind.INVALID_BOUNDARY_CHARACTER,
                    "Input begins or ends in an invalid character: $input",
                )
            }
            val value = toPlain(input)
            if (value.length < SHORTEST_POSSIBLE_IBAN) {
                return Rejection.Malformed(value, Kind.TOO_SHORT)
            }
            if (!(value[2].isAsciiDigit() && value[3].isAsciiDigit())) {
                return Rejection.Malformed(value, Kind.NON_NUMERIC_CHECK_DIGITS)
            }
            val countryCode: String = value.substring(0, 2)
            val expectedLength: Int =
                CountryCodes.getLength(countryCode)
                    ?: return if (isKnownCountryCodeInWrongCase(countryCode)) {
                        Rejection.Malformed(value, Kind.NON_UPPER_CASE_COUNTRY_CODE)
                    } else {
                        Rejection.UnknownCountryCode(value, countryCode)
                    }
            if (expectedLength != value.length) {
                return Rejection.WrongLength(value, expectedLength, value.length)
            }
            val calculatedChecksum: Int =
                try {
                    Modulo97.checksum(value)
                } catch (e: IllegalArgumentException) {
                    return Rejection.Malformed(
                        value,
                        Kind.INVALID_CHARACTER,
                        e.message ?: "Invalid character in $value",
                    )
                }
            if (calculatedChecksum != 1) {
                return Rejection.WrongChecksum(value)
            }
            return null
        }

        /**
         * Composes an IBAN from the given country code and basic bank account number, calculating
         * the check digits.
         *
         * @param countryCode the country code.
         * @param bban the BBAN.
         * @return the IBAN object composed of the given parts.
         * @throws IbanParseException describing why the parts were rejected.
         */
        @Throws(IbanParseException::class)
        fun compose(countryCode: CharSequence, bban: CharSequence): Iban {
            val sb =
                StringBuilder(CountryCodes.LONGEST_IBAN_LENGTH)
                    .append(countryCode)
                    .append("00")
                    .append(bban)
            val checkDigits =
                try {
                    Modulo97.calculateCheckDigits(sb)
                } catch (e: IllegalArgumentException) {
                    throw IbanParseException.Malformed(
                        toPlain(sb),
                        Kind.INVALID_STRUCTURE,
                        e.message ?: "Cannot calculate check digits for $sb",
                    )
                }
            sb.setRange(2, 4, checkDigits.toString().padStart(2, '0'))
            return invoke(sb)
        }

        /**
         * Whether the given two-character country code is a known IBAN country code that was
         * written in the wrong case. Such input stays rejected — kiban rejects rather than
         * normalizes — but it deserves a better diagnosis than "unknown country code", because the
         * country is known.
         *
         * Only called once the code has already failed the [CountryCodes.getLength] lookup, so both
         * early returns keep a doomed input from paying for a second binary search.
         */
        private fun isKnownCountryCodeInWrongCase(countryCode: String): Boolean {
            val first: Char = countryCode[0]
            val second: Char = countryCode[1]
            if (!first.isAsciiLetter() || !second.isAsciiLetter()) return false
            // An all-upper-case code that failed the lookup is genuinely unknown: upper-casing it
            // cannot change the outcome.
            if (!first.isAsciiLowerCase() && !second.isAsciiLowerCase()) return false
            return CountryCodes.getLength(
                charArrayOf(first.uppercaseAscii(), second.uppercaseAscii()).concatToString()
            ) != null
        }

        /**
         * Removes any spaces contained in the String thereby converting the input into a plain IBAN
         *
         * @param input possibly pretty printed IBAN
         * @return plain IBAN
         */
        internal fun toPlain(input: CharSequence): String =
            input.filter { !it.isWhitespace() }.toString()

        /**
         * Converts a plain to a pretty printed IBAN
         *
         * @param value plain iban
         * @return pretty printed IBAN
         */
        internal fun addSpaces(value: CharSequence): String = value.chunked(4).joinToString(" ")
    }
}

/**
 * Parses the given string into an IBAN object and confirms the check digits.
 *
 * @return the parsed and validated IBAN object.
 * @throws IbanParseException describing why the input was rejected.
 * @see Iban.invoke
 */
@Throws(IbanParseException::class)
fun String.toIban(): Iban =
    when (val rejection = Iban.validate(this)) {
        // Must stay ofValidated, not Iban(...): this is a top-level function, so Iban(...) here
        // would resolve to invoke and re-run validate() on an input already known to be valid.
        null -> Iban.ofValidated(Iban.toPlain(this))
        else -> throw rejection.toException()
    }

/**
 * Parses the given string into an IBAN object and confirms the check digits, discarding the failure
 * detail.
 *
 * @return the parsed and validated IBAN object, or `null` if the input is in some way invalid.
 * @see Iban.invoke
 */
fun String.toIbanOrNull(): Iban? =
    // Must stay ofValidated, not Iban(...): see the comment on toIban above.
    if (Iban.validate(this) == null) Iban.ofValidated(Iban.toPlain(this)) else null

/** Returns whether the given string is a valid IBAN. */
fun String.isValidIban(): Boolean = Iban.validate(this) == null

/**
 * Bit 5 of an ASCII letter is its case bit: setting it maps `A`-`Z` onto `a`-`z` and clearing it
 * maps them back. Testing a letter of either case is therefore one range check rather than two, and
 * upper-casing one is a single mask rather than a trip through the Unicode case tables that
 * [Char.uppercaseChar] consults.
 */
private const val ASCII_CASE_BIT = 0x20

/**
 * Whether this is an ASCII digit. Deliberately not [Char.isDigit], which is Unicode-aware on every
 * platform and accepts fullwidth (`９`), Arabic-Indic (`٩`) and other non-ASCII digits.
 */
private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

/**
 * Whether this is an ASCII letter of either case. Deliberately not [Char.isLetter], for the same
 * reason as [isAsciiDigit]: folding the case first keeps this to a single range check, and folding
 * a non-ASCII character can only move it further outside `a`-`z`.
 */
private fun Char.isAsciiLetter(): Boolean = (code or ASCII_CASE_BIT).toChar() in 'a'..'z'

/** Whether this is a lower case ASCII letter. */
private fun Char.isAsciiLowerCase(): Boolean = this in 'a'..'z'

/**
 * Upper-cases an ASCII letter by clearing its case bit. The receiver must already be one — every
 * other character comes back mangled, not unchanged.
 */
private fun Char.uppercaseAscii(): Char = (code and ASCII_CASE_BIT.inv()).toChar()

/**
 * Whether this is an ASCII letter or digit, the character set ISO 13616 allows in an IBAN.
 * Deliberately not [Char.isLetterOrDigit], for the same reason as [isAsciiDigit].
 */
private fun Char.isAsciiLetterOrDigit(): Boolean = isAsciiDigit() || isAsciiLetter()
