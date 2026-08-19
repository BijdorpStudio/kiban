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
import de.infix.testBalloon.framework.core.testSuite

/**
 * Ensures that the [Iban] class accepts IBAN numbers from every participating country (...known at
 * the time the test was last updated).
 */
val IbanInternationalTest by testSuite {
    for (testData in countriesTestData) {
        test("Parse should accept plain form for ${testData.name}") {
            val iban = Iban(testData.plain)
            assertThat(iban.plain).isEqualTo(testData.plain)
            assertThat(iban.toString()).isEqualTo(testData.pretty)
        }
    }

    for (testData in countriesTestData) {
        test("Parse should accept pretty printed form for ${testData.name}") {
            val iban = Iban(testData.pretty)
            assertThat(iban.plain).isEqualTo(testData.plain)
            assertThat(iban.toString()).isEqualTo(testData.pretty)
        }
    }

    for (testData in countriesTestData) {
        test("Compose should round trip ${testData.name}") {
            val composed =
                Iban.compose(
                    countryCode = testData.plain.substring(0, 2),
                    bban = testData.plain.substring(4),
                )
            assertThat(composed).isEqualTo(Iban(testData.plain))
        }
    }

    for (testData in countriesTestData) {
        test("Compose should round trip through the accessors for ${testData.name}") {
            val iban = Iban(testData.plain)

            assertThat(Iban.compose(iban.countryCode, iban.bban)).isEqualTo(iban)
        }
    }

    for (testData in countriesTestData) {
        test("Check is registered IBAN for ${testData.name}") {
            assertThat(Iban(testData.plain).isInSwiftRegistry).isEqualTo(testData.swift)
        }
    }

    for (testData in countriesTestData) {
        test("Check is SEPA country for ${testData.name}") {
            assertThat(Iban(testData.plain).isSEPA).isEqualTo(testData.sepa)
        }
    }

    for (testData in countriesTestData) {
        test("Get length for country code should return correct value for ${testData.name}") {
            val lengthForCountryCode = CountryCodes.ibanLength(testData.plain.substring(0, 2)) ?: -1
            assertThat(lengthForCountryCode).isEqualTo(testData.plain.length)
        }
    }
}
