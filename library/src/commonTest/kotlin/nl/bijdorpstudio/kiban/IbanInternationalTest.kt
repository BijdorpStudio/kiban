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

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/**
 * Ensures that the [Iban] class accepts IBAN numbers from every participating country (...known at
 * the time the test was last updated).
 */
class IbanInternationalTest {
    @Test
    fun `Parse should accept plain form`() {
        CountryCodesParameterizedTest.countriesTestDataTable.forAll { testData ->
            val iban = Iban(testData.plain)
            assertThat(iban.plain).isEqualTo(testData.plain)
            assertThat(iban.toString()).isEqualTo(testData.pretty)
        }
    }

    @Test
    fun `Parse should accept pretty printed form`() {
        CountryCodesParameterizedTest.countriesTestDataTable.forAll { td ->
            val iban = Iban(td.pretty)
            assertThat(iban.plain).isEqualTo(td.plain)
            assertThat(iban.toString()).isEqualTo(td.pretty)
        }
    }

    @Test
    fun `Compose should round trip every country`() {
        CountryCodesParameterizedTest.countriesTestDataTable.forAll { td ->
            val composed =
                Iban.compose(
                    countryCode = td.plain.substring(0, 2),
                    bban = td.plain.substring(4),
                )
            assertThat(composed).isEqualTo(Iban(td.plain))
        }
    }

    @Test
    fun `Check is registered IBAN`() {
        CountryCodesParameterizedTest.countriesTestDataTable.forAll { td ->
            assertThat(Iban(td.plain).isInSwiftRegistry).isEqualTo(td.swift)
        }
    }

    @Test
    fun `Check is SEPA country`() {
        CountryCodesParameterizedTest.countriesTestDataTable.forAll { td ->
            assertThat(Iban(td.plain).isSEPA).isEqualTo(td.sepa)
        }
    }

    @Test
    fun `Get length for country code should return correct value`() {
        CountryCodesParameterizedTest.countriesTestDataTable.forAll { td ->
            val lengthForCountryCode = CountryCodes.getLength(td.plain.substring(0, 2)) ?: -1
            assertThat(lengthForCountryCode).isEqualTo(td.plain.length)
        }
    }
}
