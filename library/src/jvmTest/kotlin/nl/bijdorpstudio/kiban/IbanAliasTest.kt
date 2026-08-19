/*
  Copyright 2026 Barend Garvelink, Eugen Martynov

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
import assertk.assertions.isSameInstanceAs
import de.infix.testBalloon.framework.core.testSuite

/**
 * Tests for the JVM-only [IBAN] alias of [Iban]. Everything here is a compile-time claim as much as
 * a runtime one: the alias exists so a `java-iban` port keeps compiling with nothing but its import
 * changed, and that is what these call sites exercise.
 */
val IbanAliasTest by testSuite {
    test("IBAN alias should name the same type as Iban") {
        // No separate class is generated for a typealias, so the two spellings have to agree at
        // the class level, not merely produce equal values.
        assertThat(IBAN::class.java).isSameInstanceAs(Iban::class.java)
        assertThat(IBAN::class.java.name).isEqualTo("nl.bijdorpstudio.kiban.Iban")
    }

    test("IBAN alias should parse through the same entry point as Iban") {
        val alias: IBAN = IBAN(VALID_IBAN)
        val direct: Iban = Iban(VALID_IBAN)

        assertThat(alias).isEqualTo(direct)
        assertThat(alias.plain).isEqualTo(VALID_IBAN)
    }

    test("IBAN alias should reach the companion functions") {
        // The java-iban spellings a migrating caller is most likely to have written.
        assertThat(IBAN.parse(VALID_IBAN)).isEqualTo(Iban(VALID_IBAN))
        assertThat(IBAN.compose(countryCode = "NL", bban = VALID_IBAN.substring(4)))
            .isEqualTo(Iban(VALID_IBAN))
        assertThat(IBAN.SHORTEST_POSSIBLE_IBAN_LENGTH).isEqualTo(Iban.SHORTEST_POSSIBLE_IBAN_LENGTH)
    }

    test("IBAN and Iban should be interchangeable as declared types") {
        // Assigning across the two spellings in both directions is the whole point of the alias:
        // a migrated call site holding an `IBAN` still feeds APIs typed as `Iban`, and vice versa.
        val fromExtension: IBAN = VALID_IBAN.toIban()
        val backAsIban: Iban = fromExtension

        assertThat(fromExtension).isEqualTo(backAsIban)
        assertThat(listOf<Iban>(fromExtension).single()).isEqualTo(IBAN(VALID_IBAN))
    }
}
