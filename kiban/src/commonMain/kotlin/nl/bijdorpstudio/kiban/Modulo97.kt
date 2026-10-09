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

/** Calculates the modulo 97 checksum used in IBAN numbers (and some other entities). */
public object Modulo97 {

    /**
     * Calculates the raw MOD97 checksum for a given input.
     *
     * The input is allowed to contain space characters. Any character outside the ASCII range
     * `[A-Za-z0-9 ]` will cause an [IllegalArgumentException] to be thrown; non-ASCII digits such
     * as fullwidth `９` or Arabic-Indic `٩` are rejected rather than normalized. The checksum is
     * folded one character at a time, so it allocates nothing beyond what the caller passed in and
     * has no input size beyond which it breaks down.
     *
     * It is expected but not enforced that the characters at index 2 and 3 are numeric. If the
     * existing check digits are `00` then this method will return the value that, after subtracting
     * it from 98, gives you the check digits for a MOD-97 verifiable string. If the existing check
     * digits are any other value, this method will return `1` if the input checksums correctly.
     *
     * You may want to use [calculateCheckDigits] or [verifyCheckDigits] instead of this method.
     *
     * @param input the input, which should be at least five characters excluding spaces.
     * @return the check digits calculated for the given IBAN.
     * @throws IllegalArgumentException if the input is in some way invalid.
     * @see calculateCheckDigits
     * @see verifyCheckDigits
     */
    public fun checksum(input: CharSequence): Int {
        if (!atLeastFiveNonSpaceCharacters(input)) {
            throw IllegalArgumentException(
                "The input must be non-null and contain at least five non-space characters: $input"
            )
        }
        // Using the algorithm from
        // https://en.wikipedia.org/wiki/International_Bank_Account_Number#Modulo_operation_on_IBAN
        // The first four characters move to the end, so they are folded in last.
        val remainder: Int = fold(input, 4, input.length, 0)
        return fold(input, 0, 4, remainder)
    }

    /**
     * Calculates the check digits to be used in a MOD97 checked string.
     *
     * @param input the input; the characters at indices 2 and 3 **must** be `'0'`. The input must
     *   also satisfy the criteria defined in [checksum].
     * @return the check digits to be used at indices 2 and 3 to make the input MOD97 verifiable.
     * @throws IllegalArgumentException if the input is shorter than five characters or does not
     *   carry `'0'` at indices 2 and 3.
     */
    public fun calculateCheckDigits(input: CharSequence): Int {
        if (input.length < 5 || input[2] != '0' || input[3] != '0') {
            throw IllegalArgumentException(
                "The input must be non-null, have a minimum length of five characters, and the characters at indices 2 and 3 must be '0'. Was $input"
            )
        }
        return 98 - checksum(input)
    }

    /**
     * Calculates the check digits for a given country code and BBAN.
     *
     * @param countryCode the country code. Not validated to be a known country.
     * @param bban the country-specific BBAN. Not validated to required length.
     * @return the check digits to be used at indices 2 and 3 to make the input MOD97 verifiable.
     * @throws IllegalArgumentException if the country code is not two characters or contains a
     *   space character.
     */
    public fun calculateCheckDigits(countryCode: CharSequence, bban: CharSequence): Int {
        if (countryCode.length != 2) {
            throw IllegalArgumentException("Country code should be length 2 but was $countryCode")
        }
        if (countryCode.contains(' ')) {
            throw IllegalArgumentException(
                "Country code contains space character (0x20): $countryCode"
            )
        }
        val sb = StringBuilder(countryCode).append("00").append(bban)
        return calculateCheckDigits(sb)
    }

    /**
     * Determines whether the given input has a valid MOD97 checksum.
     *
     * @param input the input to verify, it must meet the criteria defined in [checksum].
     * @return `true` if the input passes checksum verification, `false` otherwise.
     */
    public fun verifyCheckDigits(input: CharSequence): Boolean = checksum(input) == 1

    /**
     * Folds `src[srcPos..<srcLen)` into the running MOD97 remainder, applying the character to
     * numeric transformation and skipping over space (ASCII 0x20) characters.
     *
     * Folding each digit in as `remainder = (remainder * 10 + digit) % 97` is the same arithmetic
     * as reading the whole transformed string as one number and taking it modulo 97, but it needs
     * no buffer to hold that string: a letter expands into its two digits in place. The remainder
     * never leaves `0..<97`, so no intermediate exceeds `97 * 10 + 9` and the whole fold fits in an
     * [Int].
     *
     * @param src the data to fold, must contain only ASCII characters `[A-Za-z0-9 ]`.
     * @param srcPos the index in `src` to begin folding (inclusive).
     * @param srcLen the index in `src` to stop folding (exclusive).
     * @param initial the remainder to fold into, `0` to start a fresh calculation.
     * @return the remainder after folding in every character of the range.
     * @throws IllegalArgumentException if `src` contains an unsupported character.
     */
    private fun fold(src: CharSequence, srcPos: Int, srcLen: Int, initial: Int): Int {
        var remainder = initial
        for (i in srcPos..<srcLen) {
            val c = src[i]
            remainder =
                when {
                    // Deliberately not Char.isDigit(): that is Unicode-aware and would accept
                    // fullwidth or Arabic-Indic digits, which are not part of the ISO 13616
                    // character set. See Iban.validate for the same reasoning.
                    c in '0'..'9' -> (remainder * 10 + (c.code - '0'.code)) % 97

                    c in 'A'..'Z' -> foldLetter(remainder, 10 + (c.code - 'A'.code))

                    c in 'a'..'z' -> foldLetter(remainder, 10 + (c.code - 'a'.code))

                    c == ' ' -> remainder

                    else ->
                        throw IllegalArgumentException(
                            "Invalid character '$c'. in ${src.subSequence(srcPos, srcLen)}"
                        )
                }
        }
        return remainder
    }

    /**
     * Folds the two decimal digits of a letter's numeric value into the remainder. The value is
     * always in `10..35`, the range ISO 13616 assigns to `A`-`Z`, so it always contributes exactly
     * two digits.
     */
    private fun foldLetter(remainder: Int, value: Int): Int =
        ((remainder * 10 + value / 10) % 97 * 10 + value % 10) % 97

    /**
     * Whether the input holds at least five characters that are not a space (ASCII 0x20).
     *
     * Counts rather than filtering into a new string: this runs on every checksum, and the count is
     * all the caller needs.
     */
    private fun atLeastFiveNonSpaceCharacters(input: CharSequence): Boolean {
        var seen = 0
        for (i in input.indices) {
            if (input[i] != ' ' && ++seen == 5) {
                return true
            }
        }
        return false
    }
}
