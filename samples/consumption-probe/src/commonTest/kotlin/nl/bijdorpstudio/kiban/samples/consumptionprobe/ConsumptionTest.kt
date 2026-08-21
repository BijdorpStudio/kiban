package nl.bijdorpstudio.kiban.samples.consumptionprobe

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import nl.bijdorpstudio.kiban.CountryCodes
import nl.bijdorpstudio.kiban.Iban
import nl.bijdorpstudio.kiban.IbanParseException
import nl.bijdorpstudio.kiban.Modulo97
import nl.bijdorpstudio.kiban.isValidIban
import nl.bijdorpstudio.kiban.toIban
import nl.bijdorpstudio.kiban.toIbanOrNull

/**
 * Not a test of the library. `:library`'s own suite covers behaviour across every target far more
 * thoroughly than this ever should, and every assertion here is duplicated there.
 *
 * What is under test is that the *published* artifact resolves and works: that
 * `nl.bijdorpstudio.kiban:kiban:<version>` can be resolved from a repository by a build that has
 * never heard of `:library`, that its Gradle module metadata offers a usable variant for each of
 * this build's targets, and that the declarations README.md tells consumers to call are in the
 * artifacts that came back.
 *
 * So the assertions are deliberately shallow and the surface deliberately wide - one call per kind
 * of declaration a consumer reaches for. A declaration that failed to publish shows up as a compile
 * or link error here long before an assertion on its result could fail, which is the point: the
 * interesting failures happen before any of this runs.
 */
class ConsumptionTest {
    @Test
    fun classMembersResolve() {
        val iban = Iban("NL91ABNA0417164300")

        assertEquals("NL", iban.countryCode)
        assertEquals("91", iban.checkDigits)
        assertEquals("ABNA0417164300", iban.bban)
        assertEquals("NL91ABNA0417164300", iban.plain)
        assertEquals("NL91 ABNA 0417 1643 00", iban.pretty)
        assertEquals("ABNA", iban.bankIdentifier)
        assertTrue(iban.isSEPA)
        assertTrue(iban.isInSwiftRegistry)
    }

    @Test
    fun companionEntryPointsResolve() {
        val parsed = Iban.parse("NL91ABNA0417164300")
        val composed = Iban.compose("NL", "ABNA0417164300")

        assertEquals(parsed, composed)
    }

    @Test
    fun topLevelExtensionsResolve() {
        assertTrue("NL91ABNA0417164300".isValidIban())
        assertEquals(Iban("NL91ABNA0417164300"), "NL91ABNA0417164300".toIban())
        assertNull("NL91ABNA0417164301".toIbanOrNull())
    }

    @Test
    fun exceptionHierarchyResolves() {
        val failure =
            assertFailsWith<IbanParseException.UnknownCountryCode> {
                Iban("XX82WEST12345698765432")
            }

        assertEquals("XX", failure.countryCode)
        assertEquals("XX82WEST12345698765432", failure.input)
    }

    @Test
    fun registryDataResolves() {
        // The generated country data is the bulk of the artifact and the part least likely to fail
        // as a compile error if something went wrong with packaging.
        assertTrue(CountryCodes.isKnownCountryCode("NL"))
        assertTrue(CountryCodes.knownCountryCodes.contains("NL"))
        assertEquals(18, CountryCodes.ibanLength("NL"))
        assertTrue(CountryCodes.lastUpdateRevision.isNotEmpty())
    }

    @Test
    fun modulo97Resolves() {
        assertTrue(Modulo97.verifyCheckDigits("NL91ABNA0417164300"))
        assertEquals(91, Modulo97.calculateCheckDigits("NL", "ABNA0417164300"))
    }
}
