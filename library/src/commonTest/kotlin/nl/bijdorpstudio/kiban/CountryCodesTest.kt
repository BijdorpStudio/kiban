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
import assertk.assertions.endsWith
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Some tests for [CountryCodes]. */
val CountryCodesTest by testSuite {
    test("Known country codes should not be editable") {
        // Not meant to be an exhaustive test, just a reminder to keep API consistent if
        // implementation changes. The list is built by `buildList`, which rejects mutation on
        // every target once built — so, unlike the array view it replaced, this holds outside
        // the JVM too.
        @Suppress("UNCHECKED_CAST")
        assertFailure { (CountryCodes.knownCountryCodes as MutableList<String>).add("ZZ") }
            .isInstanceOf<UnsupportedOperationException>()
    }

    test("Known country codes should not be editable in place") {
        // A `set` through the cast is what an array view (`Array.asList()`) would have allowed on
        // the JVM, corrupting the library's own reference data. The defensive copy rules it out.
        @Suppress("UNCHECKED_CAST")
        assertFailure { (CountryCodes.knownCountryCodes as MutableList<String>)[0] = "ZZ" }
            .isInstanceOf<UnsupportedOperationException>()

        assertThat(CountryCodes.knownCountryCodes.first()).isEqualTo("AD")
    }

    test("Known country codes should be in ascending order") {
        val raw = CountryCodes.knownCountryCodes

        assertThat(raw.sorted()).isEqualTo(raw)
    }

    test("Known country codes should be the same instance on every access") {
        assertThat(CountryCodes.knownCountryCodes).isSameInstanceAs(CountryCodes.knownCountryCodes)
    }

    test("Known country codes should agree with isKnownCountryCode") {
        assertThat(CountryCodes.knownCountryCodes.all { CountryCodes.isKnownCountryCode(it) })
            .isTrue()
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

    test("ibanLength returns null for unknown country code") {
        assertThat(CountryCodes.ibanLength("XX")).isNull()
    }

    test("ibanLength returns the registered length for a known country code") {
        assertThat(CountryCodes.ibanLength("NL")).isEqualTo(18)
    }

    test("shortest and longest IBAN lengths bound every known country") {
        val lengths = CountryCodes.knownCountryCodes.map { CountryCodes.ibanLength(it) }

        assertThat(lengths.minOf { it ?: 0 }).isEqualTo(CountryCodes.shortestIbanLength)
        assertThat(lengths.maxOf { it ?: 0 }).isEqualTo(CountryCodes.longestIbanLength)
    }

    test("shortest known IBAN length is not shorter than the technical minimum") {
        assertThat(CountryCodes.shortestIbanLength)
            .isGreaterThanOrEqualTo(Iban.SHORTEST_POSSIBLE_IBAN_LENGTH)
    }

    test("lastUpdateDate should not be null") {
        assertThat(CountryCodes.lastUpdateDate)
            .isNotNull()
            .isBetween(
                start = Instant.DISTANT_PAST,
                end = Clock.System.now(),
            )
    }

    // The midnight-UTC encoding of lastUpdateDate is a frozen part of the public contract (#144):
    // a registry release is dated to the day, and with no LocalDate in the standard library the
    // date is carried as the instant at 00:00:00Z. Callers are documented to rely on that, so it
    // is pinned here rather than left as an implementation detail of the getter.
    test("lastUpdateDate is the registry date at exactly midnight UTC") {
        val lastUpdateDate = CountryCodes.lastUpdateDate

        assertThat(lastUpdateDate.nanosecondsOfSecond).isEqualTo(0)
        assertThat(lastUpdateDate.epochSeconds % 1.days.inWholeSeconds).isEqualTo(0L)
    }

    test("lastUpdateDate renders as an ISO-8601 instant and round-trips") {
        val rendered = CountryCodes.lastUpdateDate.toString()

        assertThat(rendered).endsWith("T00:00:00Z")
        assertThat(Instant.parse(rendered)).isEqualTo(CountryCodes.lastUpdateDate)
    }

    test("lastUpdateDate is a stable value across reads") {
        // The property is a getter that re-parses on every call, so equality across two reads is
        // worth asserting: an Instant is a value, and callers may compare results of separate
        // reads.
        assertThat(CountryCodes.lastUpdateDate).isEqualTo(CountryCodes.lastUpdateDate)
    }

    test("lastUpdateRevision should not be null") {
        assertThat(CountryCodes.lastUpdateRevision).isNotNull()
    }
}
