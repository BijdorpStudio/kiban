package nl.garvelink.oss.iban

import org.junit.Test
import org.junit.Assert.*
import nl.bijdorpstudio.kiban.Modulo97 as KibanModulo97 // For checking ReplaceWith

class Modulo97CompatTest {

    private val validIbanString = "NL91ABNA0417164300"
    private val invalidIbanStringCheckDigit = "NL92ABNA0417164300" // Invalid check digit

    // For calculateCheckDigits(CharSequence), kiban expects countryCode + bban
    private val countryAndBbanForCalc = "NLABNA0417164300" // No check digits
    private val countryCode = "NL"
    private val bban = "ABNA0417164300"
    private val expectedCheckDigitsInt = 91 // "91" for NL...ABNA0417164300

    @Test
    fun testVerifyCheckDigits_valid() {
        assertTrue(Modulo97.verifyCheckDigits(validIbanString))
    }

    @Test
    fun testVerifyCheckDigits_invalid() {
        assertFalse(Modulo97.verifyCheckDigits(invalidIbanStringCheckDigit))
    }

    @Test
    fun testCalculateCheckDigits_charSequence() {
        val checkDigits = Modulo97.calculateCheckDigits(countryAndBbanForCalc)
        assertEquals(expectedCheckDigitsInt, checkDigits)
    }

    @Test
    fun testCalculateCheckDigits_countryAndBban() {
        val checkDigits = Modulo97.calculateCheckDigits(countryCode, bban)
        assertEquals(expectedCheckDigitsInt, checkDigits)
    }

    @Test
    fun testObjectIsDeprecated() {
        val annotation = Modulo97::class.java.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation)
        assertEquals("Modulo97", annotation.replaceWith.expression)
        assertTrue(annotation.replaceWith.imports.contains("nl.bijdorpstudio.kiban.Modulo97"))
    }

    private fun checkMethodDeprecation(methodName: String, vararg params: Class<*>) {
        val method = Modulo97::class.java.getMethod(methodName, *params)
        val annotation = method.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation)
        assertTrue(annotation.replaceWith.expression.startsWith("Modulo97.")) // Target class name
        assertTrue(annotation.replaceWith.imports.contains("nl.bijdorpstudio.kiban.Modulo97"))
    }

    @Test
    fun testMethodsAreDeprecated() {
        checkMethodDeprecation("verifyCheckDigits", CharSequence::class.java)
        checkMethodDeprecation("calculateCheckDigits", CharSequence::class.java)
        checkMethodDeprecation("calculateCheckDigits", String::class.java, String::class.java)
    }
}
