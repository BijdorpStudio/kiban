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
 * Ensures that the [Iban] class accepts IBAN numbers from every participating country (...known at the time the test was last updated).
 */
class CountryCodesIbanFieldsTest {
    @Test
    fun `Should extract bank identifier`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                val iban = Iban.parse(td.plain)
                assertThat(CountryCodes.getBankIdentifier(iban)).isEqualTo(td.bank)
            }
    }

    @Test
    fun `Should extract branch identifier`() {
        CountryCodesParameterizedTest
            .countriesTestDataTable
            .forAll { td ->
                val iban = Iban.parse(td.plain)
                assertThat(CountryCodes.getBranchIdentifier(iban)).isEqualTo(td.branch)
            }
    }
}