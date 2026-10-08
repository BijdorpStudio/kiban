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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite

private const val VALID_COUNTRY = "NL"
private const val VALID_BBAN = "ABNA0417164300"

/** Test suite for [Modulo97]. */
val Modulo97Test by testSuite {
    for ((label, input) in
        listOf(
            "length 0" to "",
            "length 1" to "M",
            "length 2" to "MO",
            "length 3" to "MO9",
            "length 4" to "MO97",
            "length 1 padded to 5" to "M    ",
            "length 2 padded to 5" to "   MO",
            "length 3 padded to 5" to "M O 9",
            "length 4 padded to 5" to " MO97",
            "invalid non-whitespace" to "TS00☠",
            "invalid whitespace" to "MO97\tA",
            "fullwidth digits" to "MO００T",
            "Arabic-Indic digits" to "MO٠٠T",
            "Devanagari digits" to "MO००T",
        )) {
        test("It should reject $label") {
            assertFailure { Modulo97.checksum(input) }.isInstanceOf<IllegalArgumentException>()
        }
    }

    test("It should calculate an expected checksum") {
        assertThat(Modulo97.checksum("MO00T")).isEqualTo(83)
    }

    test("It should ignore case") {
        assertThat(Modulo97.checksum("MO00T")).isEqualTo(83)
        assertThat(Modulo97.checksum("mo00t")).isEqualTo(83)
    }

    test("It should calculate an expected check digits") {
        assertThat(Modulo97.calculateCheckDigits("MO00T")).isEqualTo(15)
    }

    test("It should return 1 for a known correct checksum") {
        assertThat(Modulo97.checksum("MO15T")).isEqualTo(1)
    }

    test("It should verify a known correct checksum") {
        assertThat(Modulo97.verifyCheckDigits("MO15T")).isTrue()
        for (i in 0 until 15) {
            val verifyResult = Modulo97.verifyCheckDigits("MO${i.toString().padStart(2, '0')}T")
            assertThat(verifyResult).isFalse()
        }
        for (i in 16 until 100) {
            val verifyResult = Modulo97.verifyCheckDigits("MO${i.toString().padStart(2, '0')}T")
            assertThat(verifyResult).isFalse()
        }
    }

    test("Should verify correct Iban") {
        val verifyResult = Modulo97.verifyCheckDigits(VALID_IBAN)
        assertThat(verifyResult).isTrue()
    }

    test("It should refuse to calculate check digits if index 2 is not 0") {
        assertFailure { Modulo97.calculateCheckDigits("MO10A") }
            .isInstanceOf<IllegalArgumentException>()
    }

    test("It should refuse to calculate check digits if index 3 is not 0") {
        assertFailure { Modulo97.calculateCheckDigits("MO02A") }
            .isInstanceOf<IllegalArgumentException>()
    }

    test("Compose should handle IBAN valid input") {
        val checkDigits =
            Modulo97.calculateCheckDigits(
                countryCode = VALID_COUNTRY,
                bban = VALID_BBAN,
            )
        assertThat(checkDigits).isEqualTo(91)
    }

    test("Compose should reject blank country code") {
        assertFailure {
            Modulo97.calculateCheckDigits(
                countryCode = "  ",
                bban = VALID_BBAN,
            )
        }
            .isInstanceOf<IllegalArgumentException>()
    }

    test("Compose should reject malformed country code") {
        assertFailure {
            Modulo97.calculateCheckDigits(
                countryCode = "potato",
                bban = VALID_BBAN,
            )
        }
            .isInstanceOf<IllegalArgumentException>()
    }

    test("Compose should accept unknown country code") {
        val checkDigits =
            Modulo97.calculateCheckDigits(
                countryCode = "XX",
                bban = "X",
            )
        assertThat(checkDigits).isEqualTo(72)
    }

    test("Compose should accept wrong length BBAN") {
        val checkDigits =
            Modulo97.calculateCheckDigits(
                countryCode = VALID_COUNTRY,
                bban = VALID_BBAN.substring(1),
            )
        assertThat(checkDigits).isEqualTo(50)
    }

    test("Single pass checksum should match the reference implementation for every country") {
        for (testData in countriesTestData) {
            assertThat(Modulo97.checksum(testData.plain), "plain ${testData.name}")
                .isEqualTo(referenceChecksum(testData.plain))
            assertThat(Modulo97.checksum(testData.pretty), "pretty ${testData.name}")
                .isEqualTo(referenceChecksum(testData.pretty))
        }
    }

    test("Single pass checksum should match the reference implementation for every letter") {
        for (letter in 'A'..'Z') {
            for (input in
                listOf("MO00$letter", "MO00${letter + ASCII_CASE_BIT}", "${letter}O00T")) {
                assertThat(Modulo97.checksum(input), input).isEqualTo(referenceChecksum(input))
            }
        }
    }

    test("Single pass checksum should match the reference implementation for a huge input") {
        // The buffer the checksum used to allocate was twice the input length, which put a ceiling
        // on how large an input it could take. Folding the remainder has none, so an input orders
        // of magnitude longer than any IBAN is no longer a special case.
        val input = buildString { repeat(1_000) { append(IBAN_CHARACTER_SET) } }
        assertThat(Modulo97.checksum(input)).isEqualTo(referenceChecksum(input))
    }
}

/** The ISO 13616 IBAN character set, in the order the numeric transformation assigns values. */
private const val IBAN_CHARACTER_SET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

/** The offset between an upper case ASCII letter and its lower case counterpart. */
private const val ASCII_CASE_BIT = 0x20

/**
 * The buffer-and-chunk implementation [Modulo97.checksum] carried before it folded the remainder in
 * a single pass: it expands the input into a buffer of digits, reads that back as a string and
 * folds it nine characters at a time through [Long] arithmetic.
 *
 * Kept here as an independent oracle. It pins the single-pass arithmetic to the algorithm it
 * replaced, which is the whole claim of that rewrite — a test computing the expectation the same
 * way the implementation does would agree with any shared mistake.
 */
private fun referenceChecksum(input: CharSequence): Int {
    val buffer = CharArray(input.length * 2)
    var offset: Int = referenceTransform(input, 4, input.length, buffer, 0)
    offset = referenceTransform(input, 0, 4, buffer, offset)
    return buffer
        .concatToString(0, offset)
        .chunked(9)
        .fold(0L) { acc, chunk -> (acc.toString() + chunk).toLong() % 97 }
        .toInt()
}

/** Expands `src[srcPos..<srcLen)` into `dest`, as the previous implementation did. */
private fun referenceTransform(
    src: CharSequence,
    srcPos: Int,
    srcLen: Int,
    dest: CharArray,
    destPos: Int,
): Int {
    var offset = destPos
    for (i in srcPos..<srcLen) {
        val c = src[i]
        when {
            c in '0'..'9' -> dest[offset++] = c
            c in 'A'..'Z' || c in 'a'..'z' -> {
                val tmp = 10 + ((c.code or ASCII_CASE_BIT) - 'a'.code)
                dest[offset++] = ('0'.code + tmp / 10).toChar()
                dest[offset++] = ('0'.code + tmp % 10).toChar()
            }
            c != ' ' -> throw IllegalArgumentException("Invalid character '$c'.")
        }
    }
    return offset
}
