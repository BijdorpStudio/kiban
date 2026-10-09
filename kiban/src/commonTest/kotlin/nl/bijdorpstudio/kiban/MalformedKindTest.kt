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
import assertk.assertions.isNotEqualTo
import de.infix.testBalloon.framework.core.testSuite
import nl.bijdorpstudio.kiban.IbanParseException.Malformed.Kind

/**
 * Tests for the value semantics of the data-carrying [Kind] subtypes. They are plain classes rather
 * than `data class`es, so that a consumer cannot construct or `copy` a kind the parser never
 * produced; `equals`, `hashCode` and `toString` are written out by hand instead, and these tests
 * are what keeps them honest.
 */
val MalformedKindTest by testSuite {
    test("InvalidBoundaryCharacter equals another carrying the same character and end") {
        assertThat(Kind.InvalidBoundaryCharacter(' ', atStart = true))
            .isEqualTo(Kind.InvalidBoundaryCharacter(' ', atStart = true))
    }

    test("InvalidBoundaryCharacter differs on the character") {
        assertThat(Kind.InvalidBoundaryCharacter(' ', atStart = true))
            .isNotEqualTo(Kind.InvalidBoundaryCharacter('!', atStart = true))
    }

    test("InvalidBoundaryCharacter differs on the end it sits at") {
        assertThat(Kind.InvalidBoundaryCharacter(' ', atStart = true))
            .isNotEqualTo(Kind.InvalidBoundaryCharacter(' ', atStart = false))
    }

    test("InvalidBoundaryCharacter hashes equal values alike") {
        assertThat(Kind.InvalidBoundaryCharacter('!', atStart = false).hashCode())
            .isEqualTo(Kind.InvalidBoundaryCharacter('!', atStart = false).hashCode())
    }

    test("InvalidBoundaryCharacter names its properties in toString") {
        assertThat(Kind.InvalidBoundaryCharacter('!', atStart = false).toString())
            .isEqualTo("InvalidBoundaryCharacter(character=!, atStart=false)")
    }

    test("InvalidCharacter equals another carrying the same character and index") {
        assertThat(Kind.InvalidCharacter('_', 6)).isEqualTo(Kind.InvalidCharacter('_', 6))
    }

    test("InvalidCharacter differs on the character") {
        assertThat(Kind.InvalidCharacter('_', 6)).isNotEqualTo(Kind.InvalidCharacter('!', 6))
    }

    test("InvalidCharacter differs on the index") {
        assertThat(Kind.InvalidCharacter('_', 6)).isNotEqualTo(Kind.InvalidCharacter('_', 7))
    }

    test("InvalidCharacter hashes equal values alike") {
        assertThat(Kind.InvalidCharacter('_', 6).hashCode())
            .isEqualTo(Kind.InvalidCharacter('_', 6).hashCode())
    }

    test("InvalidCharacter names its properties in toString") {
        assertThat(Kind.InvalidCharacter('_', 6).toString())
            .isEqualTo("InvalidCharacter(character=_, index=6)")
    }

    test("InvalidStructure equals another carrying the same reason") {
        assertThat(Kind.InvalidStructure("too short")).isEqualTo(Kind.InvalidStructure("too short"))
    }

    test("InvalidStructure differs on the reason") {
        assertThat(Kind.InvalidStructure("too short"))
            .isNotEqualTo(Kind.InvalidStructure("too long"))
    }

    test("InvalidStructure hashes equal values alike") {
        assertThat(Kind.InvalidStructure("too short").hashCode())
            .isEqualTo(Kind.InvalidStructure("too short").hashCode())
    }

    test("InvalidStructure names its property in toString") {
        assertThat(Kind.InvalidStructure("too short").toString())
            .isEqualTo("InvalidStructure(reason=too short)")
    }

    test("Kinds of different subtypes are never equal") {
        assertThat<Kind>(Kind.InvalidCharacter('_', 6)).isNotEqualTo(Kind.Empty)
    }
}
