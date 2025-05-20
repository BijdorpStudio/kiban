package nl.garvelink.oss.iban

import org.junit.Test
import org.junit.Assert.*
import java.util.Comparator
import nl.bijdorpstudio.kiban.Iban as KibanIban // For comparing results

class IBANCompatTest {

    private val validIbanString = "NL91ABNA0417164300"
    private val validIbanStringFormatted = "NL91 ABNA 0417 1643 00" // kiban formats with spaces
    private val validIbanCountry = "NL"
    private val validIbanCheckDigits = "91"
    private val validIbanBban = "ABNA0417164300"

    private val anotherValidIbanString = "DE89370400440532013000"
    private val kibanForAnotherValidIban = KibanIban(anotherValidIbanString)


    @Test
    fun testPublicConstructor_valid() {
        val iban = IBAN(validIbanString)
        assertNotNull(iban.actualKibanIban)
        assertEquals(validIbanString, iban.actualKibanIban.toPlainString())
        assertTrue(IBAN::class.java.getConstructor(String::class.java).isAnnotationPresent(Deprecated::class.java))
    }

    @Test
    fun testValueOf_valid() {
        val iban = IBAN.valueOf(validIbanString)
        assertNotNull(iban)
        assertEquals(validIbanString, iban!!.actualKibanIban.toPlainString())
        assertTrue(IBAN.Companion::class.java.getMethod("valueOf", String::class.java).isAnnotationPresent(Deprecated::class.java))
    }

    @Test
    fun testValueOf_invalid() {
        val iban = IBAN.valueOf("INVALID")
        assertNull(iban) // kiban.Iban.valueOf returns null for invalid
    }

    @Test
    fun testValueOf_null() {
        // kiban.Iban.valueOf(null) would throw an exception if null is not allowed by its contract.
        // Assuming kiban's valueOf handles null by returning null, or as per its specific contract.
        // Based on kiban's Iban.kt, valueOf(null) would lead to a NullPointerException if not handled.
        // However, the typical behavior for `valueOf` returning nullable is to return null for null input.
        // Let's assume kiban's valueOf is robust to null or this path is unlikely for this compat layer.
        // If kiban throws, this test should reflect that.
        // For now, assuming it might return null or that typical usage avoids explicit null.
        // If KibanIban.valueOf(null) throws, this test would need adjustment.
        // In kiban, `valueOf(null)` throws an NPE. So this test should reflect that the compat layer also does.
        var iban: IBAN? = null
        var exception: Exception? = null
        try {
            iban = IBAN.valueOf(null as String?)
        } catch (e: Exception) {
            exception = e
        }
        assertNull(iban) // Should be null as kiban's valueOf would throw, and our compat layer might catch or rethrow.
                        // kiban's Iban.valueOf(null) throws. The compat should ideally pass this through.
                        // The current IBAN.valueOf implementation does: KibanIban.valueOf(ibanString)?.let { IBAN(it) }
                        // So if KibanIban.valueOf throws, it won't reach the `?.let`.
        // This test depends on how KibanIban.valueOf handles null. If it throws, this should be an expected exception test.
        // Kiban Iban.valueOf(null) throws an exception. So this test should expect that.
        try {
            IBAN.valueOf(null as String?)
            fail("Should have thrown an exception for null input to valueOf, due to underlying kiban behavior")
        } catch (e: NullPointerException) {
            // Expected if kiban throws NPE for null
        }
    }

    @Test
    fun testParse_valid() {
        val iban = IBAN.parse(validIbanString)
        assertEquals(validIbanString, iban.actualKibanIban.toPlainString())
        assertTrue(IBAN.Companion::class.java.getMethod("parse", String::class.java).isAnnotationPresent(Deprecated::class.java))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParse_invalid() {
        IBAN.parse("NL91ABNA0417164301") // Invalid check digit, kiban.Iban.parse should throw
    }

    @Test
    fun testCompose_valid() {
        val iban = IBAN.compose(validIbanCountry, validIbanBban)
        assertEquals(validIbanString, iban.actualKibanIban.toPlainString()) // kiban.compose calculates check digits
        assertTrue(IBAN.Companion::class.java.getMethod("compose", String::class.java, String::class.java).isAnnotationPresent(Deprecated::class.java))
    }

    @Test
    fun testInstanceMethods() {
        val iban = IBAN.parse(validIbanString)
        val kibanIban = KibanIban(validIbanString) // Reference kiban object

        assertEquals(kibanIban.toString(), iban.toString()) // kiban formats with spaces
        assertTrue(IBAN::class.java.getMethod("toString").isAnnotationPresent(Deprecated::class.java))

        assertEquals(kibanIban.toPlainString(), iban.toPlainString())
        assertTrue(IBAN::class.java.getMethod("toPlainString").isAnnotationPresent(Deprecated::class.java))

        assertEquals(kibanIban.countryCode, iban.getCountryCode())
        assertTrue(IBAN::class.java.getMethod("getCountryCode").isAnnotationPresent(Deprecated::class.java))

        assertEquals(kibanIban.checkDigits, iban.getCheckDigits())
        assertTrue(IBAN::class.java.getMethod("getCheckDigits").isAnnotationPresent(Deprecated::class.java))

        assertEquals(kibanIban.bban, iban.getBban())
        assertTrue(IBAN::class.java.getMethod("getBban").isAnnotationPresent(Deprecated::class.java))

        assertEquals(kibanIban.isSEPA, iban.isSEPA())
        assertTrue(IBAN::class.java.getMethod("isSEPA").isAnnotationPresent(Deprecated::class.java))

        assertEquals(kibanIban.isInSwiftRegistry, iban.isInSwiftRegistry())
        assertTrue(IBAN::class.java.getMethod("isInSwiftRegistry").isAnnotationPresent(Deprecated::class.java))
    }

    @Test
    fun testEqualsAndHashCode() {
        val iban1 = IBAN.parse(validIbanString)
        val iban2 = IBAN.parse(validIbanString)
        val iban3 = IBAN.parse(anotherValidIbanString)

        assertEquals(iban1, iban2)
        assertNotEquals(iban1, iban3)

        assertEquals(iban1.hashCode(), iban2.hashCode())
        assertNotEquals(iban1.hashCode(), iban3.hashCode())
        
        assertNotEquals(iban1, "some string")
    }

    @Test
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    fun testLexicalOrder() {
        val iban1 = IBAN.parse(validIbanString) // NL...
        val iban2 = IBAN.parse(anotherValidIbanString) // DE...

        val comparator = IBAN.LEXICAL_ORDER as Comparator<IBAN>
        // DE should come before NL (lexicographically by plain string or kiban's compareTo)
        assertTrue(comparator.compare(iban2, iban1) < 0)
        assertTrue(comparator.compare(iban1, iban2) > 0)
        assertEquals(0, comparator.compare(iban1, iban1))

        val field = IBAN.Companion::class.java.getField("LEXICAL_ORDER")
        assertTrue(field.isAnnotationPresent(Deprecated::class.java))
    }

    @Test
    fun testClassIsDeprecated() {
        assertTrue(IBAN::class.java.isAnnotationPresent(Deprecated::class.java))
    }

    @Test
    fun testActualKibanIbanIsHiddenAndDeprecated() {
        val property = IBAN::class.java.getDeclaredMethod("getActualKibanIban")
        val annotation = property.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation)
        assertEquals(DeprecationLevel.HIDDEN, annotation.level)
    }

    // Helper to check ReplaceWith for methods - conceptual check
    private fun checkMethodDeprecation(methodName: String, vararg params: Class<*>) {
        val method = IBAN::class.java.getMethod(methodName, *params)
        assertTrue(method.isAnnotationPresent(Deprecated::class.java))
        val annotation = method.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation.replaceWith)
        assertTrue(annotation.replaceWith.expression.contains("actualKibanIban"))
    }

    private fun checkStaticMethodDeprecation(methodName: String, vararg params: Class<*>) {
        val method = IBAN.Companion::class.java.getMethod(methodName, *params)
        assertTrue(method.isAnnotationPresent(Deprecated::class.java))
        val annotation = method.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation.replaceWith)
        assertTrue(annotation.replaceWith.expression.contains("KibanIban")) // For static methods
    }

    @Test
    fun testDeprecationAnnotationsAndReplaceWith() {
        // Class
        val classAnnotation = IBAN::class.java.getAnnotation(Deprecated::class.java)
        assertEquals("Iban", classAnnotation.replaceWith.expression)
        assertTrue(classAnnotation.replaceWith.imports.contains("nl.bijdorpstudio.kiban.Iban"))

        // Constructor
        val constructor = IBAN::class.java.getConstructor(String::class.java)
        val constructorAnnotation = constructor.getAnnotation(Deprecated::class.java)
        assertTrue(constructorAnnotation.replaceWith.expression.contains("IBAN.parse(ibanString).actualKibanIban"))

        // Instance methods
        checkMethodDeprecation("toString")
        checkMethodDeprecation("toPlainString")
        checkMethodDeprecation("getCountryCode")
        checkMethodDeprecation("getCheckDigits")
        checkMethodDeprecation("getBban")
        checkMethodDeprecation("isSEPA")
        checkMethodDeprecation("isInSwiftRegistry")

        // Static methods
        checkStaticMethodDeprecation("valueOf", String::class.java)
        checkStaticMethodDeprecation("parse", String::class.java)
        checkStaticMethodDeprecation("compose", String::class.java, String::class.java)

        // LEXICAL_ORDER
        val field = IBAN.Companion::class.java.getField("LEXICAL_ORDER")
        val fieldAnnotation = field.getAnnotation(Deprecated::class.java)
        assertTrue(fieldAnnotation.replaceWith.expression.contains("actualKibanIban"))
    }
}
