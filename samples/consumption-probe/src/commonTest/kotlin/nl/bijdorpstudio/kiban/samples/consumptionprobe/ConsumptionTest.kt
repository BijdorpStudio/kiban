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
 * Shallow and wide on purpose: one call per kind of declaration a consumer reaches for, to check
 * that the published artifact resolves and carries them. `:library` covers the behaviour. A
 * declaration that failed to publish fails here as a compile or link error, before any assertion.
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
