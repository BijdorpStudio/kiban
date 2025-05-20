package nl.garvelink.oss.iban

import org.junit.Test
import org.junit.Assert.*
import java.util.Optional
import nl.bijdorpstudio.kiban.IbanFields as KibanIbanFields // For checking ReplaceWith

class IBANFieldsCompatTest {

    private val germanIbanString = "DE89370400440532013000"
    private val expectedGermanBankIdentifier = "37040044"

    // Greek IBAN from kiban's own test data (IbanFieldsTest.kt)
    // It has bank code "0110" and branch code "1250" based on kiban's parsing rules for GR.
    private val greekIbanString = "GR1601101250000000012300695"
    private val expectedGreekBankIdentifier = "0110" // As per kiban's IbanFields logic for GR
    private val expectedGreekBranchIdentifier = "1250" // As per kiban's IbanFields logic for GR

    @Test
    fun testGetBankIdentifier_germanIban() {
        val iban = IBAN.parse(germanIbanString) // Uses our compatibility IBAN
        val bankIdentifier = IBANFields.getBankIdentifier(iban)
        assertTrue(bankIdentifier.isPresent)
        assertEquals(expectedGermanBankIdentifier, bankIdentifier.get())
    }

    @Test
    fun testGetBranchIdentifier_germanIban() {
        val iban = IBAN.parse(germanIbanString)
        val branchIdentifier = IBANFields.getBranchIdentifier(iban)
        assertFalse(branchIdentifier.isPresent) // kiban's IbanFields for DE returns empty for branch
    }

    @Test
    fun testGetBankIdentifier_greekIban() {
        val iban = IBAN.parse(greekIbanString)
        val bankIdentifier = IBANFields.getBankIdentifier(iban)
        assertTrue(bankIdentifier.isPresent)
        assertEquals(expectedGreekBankIdentifier, bankIdentifier.get())
    }

    @Test
    fun testGetBranchIdentifier_greekIban() {
        val iban = IBAN.parse(greekIbanString)
        val branchIdentifier = IBANFields.getBranchIdentifier(iban)
        assertTrue(branchIdentifier.isPresent)
        assertEquals(expectedGreekBranchIdentifier, branchIdentifier.get())
    }

    @Test
    fun testObjectIsDeprecated() {
        val annotation = IBANFields::class.java.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation)
        assertEquals("IbanFields", annotation.replaceWith.expression) // Target is nl.bijdorpstudio.kiban.IbanFields
        assertTrue(annotation.replaceWith.imports.contains("nl.bijdorpstudio.kiban.IbanFields"))
    }

    private fun checkMethodDeprecation(methodName: String, vararg params: Class<*>) {
        val method = IBANFields::class.java.getMethod(methodName, *params)
        val annotation = method.getAnnotation(Deprecated::class.java)
        assertNotNull(annotation)
        assertTrue(annotation.replaceWith.expression.startsWith("KibanIbanFields."))
        assertTrue(annotation.replaceWith.imports.contains("nl.bijdorpstudio.kiban.IbanFields as KibanIbanFields"))
    }

    @Test
    fun testMethodsAreDeprecated() {
        checkMethodDeprecation("getBankIdentifier", IBAN::class.java) // Our compat IBAN
        checkMethodDeprecation("getBranchIdentifier", IBAN::class.java) // Our compat IBAN
    }
}
