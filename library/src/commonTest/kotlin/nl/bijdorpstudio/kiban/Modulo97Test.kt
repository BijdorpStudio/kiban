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
    // Inputs the checksum must reject outright: anything shorter than five characters, and
    // anything holding a character outside the accepted alphabet.
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
}
