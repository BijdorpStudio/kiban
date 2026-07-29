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

import assertk.Table1
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.tableOf
import kotlin.test.Test

/**
 * Ensures that the [Iban] class accepts IBAN numbers from every participating country
 * (...known at the time the test was last updated).
 */
class CountryCodesParameterizedTest {

    @Test
    fun `Length for country code should return correct value`() {
        countriesTestDataTable
            .forAll { testData ->
                val lengthForCountryCode = CountryCodes.getLengthForCountryCode(testData.plain.substring(0, 2))
                assertThat(lengthForCountryCode).isEqualTo(testData.plain.length)
            }
    }

    @Test
    fun `Is known country code should return true`() {
        countriesTestDataTable
            .forAll { td ->
                assertThat(CountryCodes.isKnownCountryCode(td.plain.substring(0, 2))).isTrue()
            }
    }

    @Test
    fun `All country codes should be tested`() {
        val testDataCountryCodes = testData
            .map { it.plain.substring(0, 2) }
            .toSet()

        assertThat(CountryCodes.knownCountryCodes.toSet() - testDataCountryCodes)
            .isEmpty()
    }

    companion object {
        /**
         * List of valid international IBAN's.
         * References:
         * - SWIFT: https://www.swift.com/standards/data-standards/iban
         * - IBAN.com Experimental List: https://www.iban.com/structure
         */
        val testData = countryTestData.sortedBy { it.name }


        // Table for parametrized tests
        val countriesTestDataTable =
            tableOf("Test data")
                .run {
                    var table: Table1<IbanCountryTestData>? = null
                    testData
                        .forEach {
                            table = table?.row(it) ?: row(it)
                        }
                    requireNotNull(table)
                }
    }
}
