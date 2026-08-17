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

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite

/**
 * List of valid international IBAN's, ordered by country name so the generated test cases report in
 * a stable order. References:
 * - SWIFT: https://www.swift.com/standards/data-standards/iban
 * - IBAN.com Experimental List: https://www.iban.com/structure
 */
internal val countriesTestData = countryTestData.sortedBy { it.name }

/**
 * Ensures that the [Iban] class accepts IBAN numbers from every participating country (...known at
 * the time the test was last updated).
 */
val CountryCodesParameterizedTest by testSuite {
    for (testData in countriesTestData) {
        test("Length for ${testData.name} should return correct value") {
            val lengthForCountryCode = CountryCodes.ibanLength(testData.plain.substring(0, 2)) ?: -1
            assertThat(lengthForCountryCode).isEqualTo(testData.plain.length)
        }
    }

    for (testData in countriesTestData) {
        test("${testData.name} should be a known country code") {
            assertThat(CountryCodes.isKnownCountryCode(testData.plain.substring(0, 2))).isTrue()
        }
    }

    test("All country codes should be tested") {
        val testDataCountryCodes = countriesTestData.map { it.plain.substring(0, 2) }.toSet()

        assertThat(CountryCodes.knownCountryCodes.toSet() - testDataCountryCodes).isEmpty()
    }
}
