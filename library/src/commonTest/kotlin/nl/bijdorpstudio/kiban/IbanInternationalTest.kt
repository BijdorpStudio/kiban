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
import kotlin.test.Test

/**
 * Ensures that the [Iban] class accepts IBAN numbers from every participating country (...known at the time the test was last updated).
 */
class IbanInternationalTest {
    @Test
    fun `Parse should accept plain form`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { testData ->
                val iban = Iban.parse(testData.plain)
                assertThat(iban.toPlainString()).isEqualTo(testData.plain)
                assertThat(iban.toString()).isEqualTo(testData.pretty)
            }
    }

    @Test
    fun `Parse should accept pretty printed form`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                val iban = Iban.parse(td.pretty)
                assertThat(iban.toPlainString()).isEqualTo(td.plain)
                assertThat(iban.toString()).isEqualTo(td.pretty)
            }
    }

    @Test
    fun `Check is registered IBAN`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                assertThat(Iban.parse(td.plain).isInSwiftRegistry).isEqualTo(td.swift)
            }
    }

    @Test
    fun `Check is SEPA country`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                assertThat(Iban.parse(td.plain).isSEPA).isEqualTo(td.sepa)
            }
    }

    @Test
    fun `Get length for country code should return correct value`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                val lengthForCountryCode = CountryCodes.getLengthForCountryCode(td.plain.substring(0, 2))
                assertThat(lengthForCountryCode).isEqualTo(td.plain.length)
            }
    }
}
