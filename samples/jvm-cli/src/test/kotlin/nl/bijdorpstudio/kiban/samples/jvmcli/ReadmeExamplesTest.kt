package nl.bijdorpstudio.kiban.samples.jvmcli

import kotlin.test.Test
import kotlin.test.assertEquals
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
 * Executes every code example from the README's "Use" section against the published artifact
 * (resolved from mavenLocal, see build.gradle.kts), asserting the exact output the README
 * documents. This is the regression check for README claims drifting from actual behavior —
 * see #60, where `Modulo97.calculateCheckDigits("XX", "X")` was documented as 50 but actually
 * returns 72, undetected because nothing executed the snippet.
 *
 * Keep each test's assertions and comments in lockstep with README.md's "Use" section: if you
 * change one, change the other.
 */
class ReadmeExamplesTest {

    @Test
    fun `parse returns Result of Iban`() {
        // val iban: Iban = Iban.parse( "NL91ABNA0417164300" ).getOrThrow()
        val iban: Iban = Iban.parse("NL91ABNA0417164300").getOrThrow()
        assertEquals("NL91 ABNA 0417 1643 00", iban.toString())
    }

    @Test
    fun `handle failure without exceptions via fold`() {
        // Iban.parse( input ).fold( onSuccess = { accept( it ) }, onFailure = { showError(
        // it.message ) } )
        var accepted: Iban? = null
        var error: String? = null
        Iban.parse("NL91ABNA0417164300")
            .fold(onSuccess = { accepted = it }, onFailure = { error = it.message })
        assertEquals("NL91ABNA0417164300", accepted?.plain)
        assertNull(error)

        Iban.parse("not an iban")
            .fold(onSuccess = { accepted = it }, onFailure = { error = it.message })
        assertTrue(error != null)
    }

    @Test
    fun `string extensions match documented results`() {
        // val parsed: Result<Iban> = "NL91ABNA0417164300".toIban()
        val parsed: Result<Iban> = "NL91ABNA0417164300".toIban()
        assertTrue(parsed.isSuccess)

        // val orNull: Iban? = "NL91ABNA0417164301".toIbanOrNull() // null, check digits are wrong
        val orNull: Iban? = "NL91ABNA0417164301".toIbanOrNull()
        assertNull(orNull)

        // val isValid: Boolean = "NL91ABNA0417164300".isValidIban() // true
        val isValid: Boolean = "NL91ABNA0417164300".isValidIban()
        assertTrue(isValid)
    }

    @Test
    fun `failures carry a typed reason for every sealed subtype`() {
        // when ( val failure = Iban.parse( input ).exceptionOrNull() ) {
        //     is IbanParseException.UnknownCountryCode -> reportUnknown( failure.countryCode )
        //     is IbanParseException.WrongLength -> reportLength( failure.expectedLength,
        // failure.actualLength )
        //     is IbanParseException.WrongChecksum -> reportChecksum()
        //     is IbanParseException.Malformed -> reportMalformed( failure.kind )
        //     null -> Unit // parsed successfully
        // }
        fun reasonFor(input: String) =
            // exceptionOrNull()'s static type is Throwable?, not the sealed IbanParseException?,
            // so this when-as-expression needs an else even though Iban.parse always fails with
            // an IbanParseException -- the README's own when is a statement, which doesn't.
            when (val failure = Iban.parse(input).exceptionOrNull()) {
                is IbanParseException.UnknownCountryCode -> "unknown:${failure.countryCode}"
                is IbanParseException.WrongLength -> "length:${failure.expectedLength}"
                is IbanParseException.WrongChecksum -> "checksum"
                is IbanParseException.Malformed -> "malformed:${failure.kind}"
                null -> "ok"
                else -> error("unexpected failure type: $failure")
            }

        assertEquals("unknown:UU", reasonFor("UU345678345543234"))
        assertEquals("length:18", reasonFor("NL91ABNA0417164300" + "0"))
        assertEquals("checksum", reasonFor("NL92ABNA0417164300"))
        assertEquals("malformed:EMPTY", reasonFor(""))
        assertEquals("ok", reasonFor("NL91ABNA0417164300"))
    }

    @Test
    fun `toString and plain formatting match documented output`() {
        val iban = Iban.parse("NL91ABNA0417164300").getOrThrow()

        // val formatted = iban.toString() // "NL91 ABNA 0417 1643 00"
        val formatted = iban.toString()
        assertEquals("NL91 ABNA 0417 1643 00", formatted)

        // val plain = iban.plain // "NL91ABNA0417164300"
        val plain = iban.plain
        assertEquals("NL91ABNA0417164300", plain)
    }

    @Test
    fun `formatted input parses the same as compact input`() {
        // val anotherIban = Iban.parse( "BE68 5390 0754 7034" ).getOrThrow()
        val anotherIban = Iban.parse("BE68 5390 0754 7034").getOrThrow()
        assertEquals("BE68539007547034", anotherIban.plain)
    }

    @Test
    fun `Iban implements Comparable so lists sort in lexical order`() {
        // val ibans = getListOfIBANs()
        // ibans.sorted() // sorts in lexical order
        val ibans =
            listOf(
                Iban.parse("NL91ABNA0417164300").getOrThrow(),
                Iban.parse("BE68 5390 0754 7034").getOrThrow(),
            )
        assertEquals(
            listOf("BE68539007547034", "NL91ABNA0417164300"),
            ibans.sorted().map { it.plain },
        )
    }

    @Test
    fun `equals and hashCode let Iban be used as a map key`() {
        val iban = Iban.parse("NL91ABNA0417164300").getOrThrow()

        // val ibansAsKeys = mutableMapOf<Iban, String>()
        // ibansAsKeys.put( iban, "this is fine" )
        val ibansAsKeys = mutableMapOf<Iban, String>()
        ibansAsKeys.put(iban, "this is fine")
        assertEquals("this is fine", ibansAsKeys[Iban.parse("NL91ABNA0417164300").getOrThrow()])
    }

    @Test
    fun `Modulo97 verifyCheckDigits matches documented result`() {
        // val candidate = "GB29 NWBK 6016 1331 9268 19"
        // val valid = Modulo97.verifyCheckDigits( candidate ) // true
        val candidate = "GB29 NWBK 6016 1331 9268 19"
        val valid = Modulo97.verifyCheckDigits(candidate)
        assertTrue(valid)
    }

    @Test
    fun `compose returns the documented iban`() {
        // Iban.compose( "BI", "10000100010000332045181" ).getOrThrow() //
        // BI4210000100010000332045181
        val composed = Iban.compose("BI", "10000100010000332045181").getOrThrow()
        assertEquals("BI4210000100010000332045181", composed.plain)
    }

    @Test
    fun `isSEPA and isInSwiftRegistry match documented flags`() {
        val candidate = "GB29 NWBK 6016 1331 9268 19"

        // val isSepa = Iban.parse( candidate ).getOrThrow().isSEPA // true
        val isSepa = Iban.parse(candidate).getOrThrow().isSEPA
        assertTrue(isSepa)

        // val isRegistered = Iban.parse( candidate ).getOrThrow().isInSwiftRegistry // true
        val isRegistered = Iban.parse(candidate).getOrThrow().isInSwiftRegistry
        assertTrue(isRegistered)
    }

    @Test
    fun `Modulo97 calculateCheckDigits accepts CharSequence, not just String`() {
        // val builder = StringBuilder( "LU000019400644750000" )
        // val checkDigits = Modulo97.calculateCheckDigits( builder ) // 28
        val builder = StringBuilder("LU000019400644750000")
        val checkDigits = Modulo97.calculateCheckDigits(builder)
        assertEquals(28, checkDigits)
    }

    @Test
    fun `Modulo97 calculateCheckDigits matches documented results for the two-arg overload`() {
        // Modulo97.calculateCheckDigits( "GB", "NWBK60161331926819" ) // 29
        assertEquals(29, Modulo97.calculateCheckDigits("GB", "NWBK60161331926819"))

        // Modulo97.calculateCheckDigits( "XX", "X" ) // 72
        assertEquals(72, Modulo97.calculateCheckDigits("XX", "X"))
    }

    @Test
    fun `CountryCodes getLength matches documented result`() {
        // val expectedLength: Int? = CountryCodes.getLength( "DK" ) // 18
        val expectedLength: Int? = CountryCodes.getLength("DK")
        assertEquals(18, expectedLength)
    }

    @Test
    fun `bank and branch identifiers are accessible`() {
        val iban = Iban.parse("NL91ABNA0417164300").getOrThrow()

        // val bankId: String? = iban.bankIdentifier
        // val branchId: String? = iban.branchIdentifier
        val bankId: String? = iban.bankIdentifier
        val branchId: String? = iban.branchIdentifier
        assertEquals("ABNA", bankId)
        assertNull(branchId)
    }
}
