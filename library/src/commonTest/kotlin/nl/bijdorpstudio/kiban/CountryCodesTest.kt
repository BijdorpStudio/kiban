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
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestPlatform
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import kotlin.time.Clock
import kotlin.time.Instant

/** Some tests for [CountryCodes]. */
val CountryCodesTest by testSuite {
    test(
        "Known country codes should not be editable",
        // JVM only. There, Collection and MutableCollection erase to the same type, so the cast
        // is a runtime no-op and `add` reaches the fixed-size list backing `asList()`, which
        // rejects it with UnsupportedOperationException. Every other target checks the cast and
        // throws ClassCastException first — the collection is just as immutable there, but this
        // test asserts the JVM's particular way of saying so.
        testConfig =
            if (testPlatform.type == TestPlatform.Type.Jvm) TestConfig else TestConfig.disable(),
    ) {
        // Not meant to be an exhaustive test, just a reminder to keep API consistent if
        // implementation changes.
        @Suppress("UNCHECKED_CAST")
        assertFailure { (CountryCodes.knownCountryCodes as MutableCollection<String>).add("ZZ") }
            .isInstanceOf<UnsupportedOperationException>()
    }

    test("Known country codes should be in ascending order") {
        val raw = CountryCodes.knownCountryCodes

        assertThat(raw.sorted()).isEqualTo(raw)
    }

    test("isKnownCountryCode should return false for empty string") {
        assertThat(CountryCodes.isKnownCountryCode("")).isFalse()
    }

    test("isKnownCountryCode should return false for lowercase") {
        assertThat(CountryCodes.isKnownCountryCode("nl")).isFalse()
    }

    test("isKnownCountryCode should return true for existing country code") {
        assertThat(CountryCodes.isKnownCountryCode("NL")).isTrue()
    }

    test("getLength returns null for unknown country code") {
        assertThat(CountryCodes.getLength("XX")).isNull()
    }

    test("lastUpdateDate should not be null") {
        assertThat(CountryCodes.lastUpdateDate)
            .isNotNull()
            .isBetween(
                start = Instant.DISTANT_PAST,
                end = Clock.System.now(),
            )
    }

    test("lastUpdateRevision should not be null") {
        assertThat(CountryCodes.lastUpdateRevision).isNotNull()
    }
}
