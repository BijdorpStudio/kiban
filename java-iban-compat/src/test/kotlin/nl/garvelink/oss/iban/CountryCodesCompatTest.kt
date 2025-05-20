package nl.garvelink.oss.iban

import org.junit.Test
import org.junit.Assert.*
import nl.bijdorpstudio.kiban.CountryCodes as KibanCountryCodes // For checking ReplaceWith

class CountryCodesCompatTest {

    @Test
    fun testGetLengthForCountryCode_knownCodes() {
        assertEquals(18, CountryCodes.getLengthForCountryCode("NL"))
        assertEquals(22, CountryCodes.getLengthForCountryCode("DE"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetLengthForCountryCode_unknownCode() {
        CountryCodes.getLengthForCountryCode("XX") // kiban returns null, compat layer throws IllegalArgumentException
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetLengthForCountryCode_invalidCodeFormat() {
        CountryCodes.getLengthForCountryCode("NLD") // Invalid format, kiban might return null or throw
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetLengthForCountryCode_nullCode() {
        // Assuming KibanCountryCodes.getLengthForCountryCode(null) would throw or return null leading to exception
        CountryCodes.getLengthForCountryCode(null as String)
    }


    @Test
    fun testObjectIsDeprecated() {
        val annotation = CountryCodes::class.java.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation)
        assertEquals("CountryCodes", annotation.replaceWith.expression)
        assertTrue(annotation.replaceWith.imports.contains("nl.bijdorpstudio.kiban.CountryCodes"))
    }

    @Test
    fun testMethodIsDeprecated() {
        val method = CountryCodes::class.java.getMethod("getLengthForCountryCode", String::class.java)
        val annotation = method.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation)
        assertEquals("CountryCodes.getLengthForCountryCode(countryCode)", annotation.replaceWith.expression)
        assertTrue(annotation.replaceWith.imports.contains("nl.bijdorpstudio.kiban.CountryCodes"))
    }
}
