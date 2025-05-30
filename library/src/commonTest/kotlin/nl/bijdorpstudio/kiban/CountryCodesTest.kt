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
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Some tests for [CountryCodes].
 */
class CountryCodesTest {

    @Ignore // Fails on Kotlin native
    @Test
    fun `Known country codes should not be editable`() {
        // Not meant to be an exhaustive test, just a reminder to keep API consistent if implementation changes.
        assertFailsWith<UnsupportedOperationException> {
            (CountryCodes.knownCountryCodes as MutableCollection<String>).add("ZZ")
        }
    }

    @Test
    fun `Known country codes should be in ascending order`() {
        val raw = CountryCodes.knownCountryCodes

        assertThat(raw.sorted()).isEqualTo(raw)
    }

    @Test
    fun `isKnownCountryCode should return false for empty string`() {
        assertThat(CountryCodes.isKnownCountryCode("")).isFalse()
    }

    @Test
    fun `isKnownCountryCode should return false for lowercase`() {
        assertThat(CountryCodes.isKnownCountryCode("nl")).isFalse()
    }

    @Test
    fun `isKnownCountryCode should return true for existing country code`() {
        assertThat(CountryCodes.isKnownCountryCode("NL")).isTrue()
    }

    @Test
    fun `getLengthForCountryCode returns -1 for unknown country code`() {
        assertThat(CountryCodes.getLengthForCountryCode("XX")).isEqualTo(-1)
    }

// Uncomment when Kotlin 2.1 is target
//    @OptIn(ExperimentalTime::class)
//    @Test
//    fun `lastUpdateDate should not be null`() {
//        assertThat(CountryCodes.lastUpdateDate)
//            .isNotNull()
//            .isBetween(
//                start = Instant.DISTANT_PAST,
//                end = Clock.System.now()
//            )
//    }

    @Test
    fun `lastUpdateDate should not be null`() {
        assertThat(CountryCodes.lastUpdateDate).isNotNull()
    }

    @Test
    fun `lastUpdateRevision should not be null`() {
        assertThat(CountryCodes.lastUpdateRevision).isNotNull()
    }
}