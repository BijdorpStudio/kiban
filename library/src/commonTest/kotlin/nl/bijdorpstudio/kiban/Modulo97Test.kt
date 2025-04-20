/*
   Original copyright 2021 Barend Garvelink

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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Test suite for [Modulo97].
 */
class Modulo97Test {

    @Test
    fun `It should reject length 0`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("")
        }
    }

    @Test
    fun `It should reject length 1`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("M")
        }
    }

    @Test
    fun `It should reject length 2`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("MO")
        }
    }

    @Test
    fun `It should reject length 3`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("MO9")
        }
    }

    @Test
    fun `It should reject length 4`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("MO97")
        }
    }

    @Test
    fun `It should reject length 1 padded to 5`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("M    ")
        }
    }

    @Test
    fun `It should reject length 2 padded to 5`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("   MO")
        }
    }

    @Test
    fun `It should reject length 3 padded to 5`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("M O 9")
        }
    }

    @Test
    fun `It should reject length 4 padded to 5`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum(" MO97")
        }
    }

    @Test
    fun `It should reject invalid non-whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("TS00☠")
        }
    }

    @Test
    fun `It should reject invalid whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.checksum("MO97\tA")
        }
    }

    @Test
    fun `It should calculate an expected checksum`() {
        assertThat(Modulo97.checksum("MO00T")).isEqualTo(83)
    }

    @Test
    fun `It should ignore case`() {
        assertThat(Modulo97.checksum("MO00T")).isEqualTo(83)
        assertThat(Modulo97.checksum("mo00t")).isEqualTo(83)
    }

    @Test
    fun `It should calculate an expected check digits`() {
        assertThat(Modulo97.calculateCheckDigits("MO00T")).isEqualTo(15)
    }

    @Test
    fun `It should return 1 for a known correct checksum`() {
        assertThat(Modulo97.checksum("MO15T")).isEqualTo(1)
    }

    @Test
    fun `It should verify a known correct checksum`() {
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

    @Test
    fun `Should verify correct Iban`() {
        val verifyResult = Modulo97.verifyCheckDigits(VALID_BBAN)
        assertThat(verifyResult).isTrue()
    }

    @Test
    fun `It should refuse to calculate check digits if index 2 is not 0`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.calculateCheckDigits("MO10A")
        }
    }

    @Test
    fun `It should refuse to calculate check digits if index 3 is not 0`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.calculateCheckDigits("MO02A")
        }
    }

    @Test
    fun `Compose should handle IBAN valid input`() {
        val checkDigits = Modulo97.calculateCheckDigits(
            countryCode = VALID_COUNTRY,
            bban = VALID_BBAN
        )
        assertThat(checkDigits).isEqualTo(91)
    }

    @Test
    fun `Compose should reject blank country code`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.calculateCheckDigits(
                countryCode = "  ",
                bban = VALID_BBAN
            )
        }
    }

    @Test
    fun `Compose should reject malformed country code`() {
        assertFailsWith<IllegalArgumentException> {
            Modulo97.calculateCheckDigits(
                countryCode = "potato",
                bban = VALID_BBAN
            )
        }
    }

    @Test
    fun `Compose should accept unknown country code`() {
        val checkDigits = Modulo97.calculateCheckDigits(
            countryCode = "XX",
            bban = "X"
        )
        assertThat(checkDigits).isEqualTo(72)
    }

    @Test
    fun `Compose should accept wrong length BBAN`() {
        val checkDigits = Modulo97.calculateCheckDigits(
            countryCode = VALID_COUNTRY,
            bban = VALID_BBAN.substring(1)
        )
        assertThat(checkDigits).isEqualTo(50)
    }

    companion object {
        private const val VALID_COUNTRY = "NL"
        private const val VALID_BBAN = "ABNA0417164300"
    }
}
