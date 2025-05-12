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

/**
 * Calculates the modulo 97 checksum used in IBAN numbers (and some other entities).
 */
object Modulo97 {

    /**
     * Calculates the raw MOD97 checksum for a given input.
     *
     *
     * The input is allowed to contain space characters. Any character outside the range `[A-Za-z0-9 ]` will cause
     * an [IllegalArgumentException] to be thrown. This method allocates a temporary buffer of twice the input
     * length, so it will fail for unreasonably large inputs.
     *
     *
     * It is expected but not enforced that the characters at index 2 and 3 are numeric. If the existing check digits
     * are `00` then this method will return the value that, after subtracting it from 98, gives you the check
     * digits for a MOD-97 verifiable string. If the existing check digits are any other value, this method will return
     * `1` if the input checksums correctly.
     *
     *
     * You may want to use [calculateCheckDigits] or [verifyCheckDigits]
     * instead of this method.
     *
     * @param input the input, which should be at least five characters excluding spaces.
     * @return the check digits calculated for the given IBAN.
     * @throws [IllegalArgumentException] if the input is in some way invalid.
     * @see [calculateCheckDigits]
     * @see [verifyCheckDigits]
     */
    fun checksum(input: CharSequence): Int {
        if (!atLeastFiveNonSpaceCharacters(input)) {
            throw IllegalArgumentException("The input must be non-null and contain at least five non-space characters: $input")
        }
        val buffer = CharArray(input.length * 2)
        var offset: Int = transform(input, 4, input.length, buffer, 0)
        offset = transform(input, 0, 4, buffer, offset)

        // Using the algorithm from https://en.wikipedia.org/wiki/International_Bank_Account_Number#Modulo_operation_on_IBAN
        // Process the string of 19 digits at a time as integers
        val remainder = buffer
            .concatToString(0, offset)
            .chunked(19)
            .fold(0L) {acc, chunk ->
                (acc.toString() + chunk).toLong() % 97
            }

        return remainder.toInt()
    }

    /**
     * Calculates the check digits to be used in a MOD97 checked string.
     * @param input the input; the characters at indices 2 and 3 **must** be `'0'`. The input must
     * also satisfy the criteria defined in [.checksum].
     * @return the check digits to be used at indices 2 and 3 to make the input MOD97 verifiable.
     */
    fun calculateCheckDigits(input: CharSequence): Int {
        if (input.length < 5 || input[2] != '0' || input[3] != '0') {
            throw IllegalArgumentException("The input must be non-null, have a minimum length of five characters, and the characters at indices 2 and 3 must be '0'. Was $input")
        }
        return 98 - checksum(input)
    }

    /**
     * Calculates the check digits for a given country code and BBAN.
     * @param countryCode the country code. Not validated to be a known country.
     * @param bban the country-specific BBAN. Not validated to required length.
     * @return the check digits to be used at indices 2 and 3 to make the input MOD97 verifiable.
     * @throws [IllegalArgumentException] if the country code is not two characters or contains a space character.
     * @since 1.9.0
     */
    fun calculateCheckDigits(countryCode: CharSequence, bban: CharSequence): Int {
        if (countryCode.length != 2) {
            throw IllegalArgumentException("Country code should be length 2 but was $countryCode")
        }
        if (countryCode.contains(' ')) {
            throw IllegalArgumentException("Country code contains space character (0x20): $countryCode")
        }
        val sb = StringBuilder(countryCode).append("00").append(bban)
        return calculateCheckDigits(sb)
    }

    /**
     * Determines whether the given input has a valid MOD97 checksum.
     * @param input the input to verify, it must meet the criteria defined in [checksum].
     * @return `true` if the input passes checksum verification, `false` otherwise.
     */
    fun verifyCheckDigits(input: CharSequence): Boolean = checksum(input) == 1

    /**
     * Copies `src[srcPos...srcLen)` into `dest[destPos)` while applying character to numeric transformation and skipping over space (ASCII 0x20) characters.
     * @param src the data to begin copying, must contain only characters `[A-Za-z0-9 ]`.
     * @param srcPos the index in `src` to begin transforming (inclusive).
     * @param srcLen the number of characters starting from `srcPos` to transform.
     * @param dest the buffer to write transformed characters into.
     * @param destPos the index in `dest` to begin writing.
     * @return the value of `destPos` incremented by the number of characters that were added, i.e. the next unused index in `dest`.
     * @throws [IllegalArgumentException] if `src` contains an unsupported character.
     * @throws [IndexOutOfBoundsException] if `dest` does not have enough capacity to store the transformed result (keep in mind that a single character from `src` can expand to two characters in `dest`).
     */
    private fun transform(src: CharSequence, srcPos: Int, srcLen: Int, dest: CharArray, destPos: Int): Int {
        var offset = destPos
        for (i in srcPos..<srcLen) {
            val c = src[i]
            when {
                c.isDigit() -> {
                    dest[offset++] = c
                }

                c in 'A'..'Z' -> {
                    val tmp = 10 + (c.code - 'A'.code)
                    dest[offset++] = ('0'.code + tmp / 10).toChar()
                    dest[offset++] = ('0'.code + tmp % 10).toChar()
                }

                c in 'a'..'z' -> {
                    val tmp = 10 + (c.code - 'a'.code)
                    dest[offset++] = ('0'.code + tmp / 10).toChar()
                    dest[offset++] = ('0'.code + tmp % 10).toChar()
                }

                c != ' ' -> {
                    throw IllegalArgumentException("Invalid character '$c'. in ${src.subSequence(srcPos, srcLen)}")
                }
            }
        }
        return offset
    }

    private fun atLeastFiveNonSpaceCharacters(input: CharSequence): Boolean =
        input
            .filter { it != ' ' }
            .length >= 5
}
