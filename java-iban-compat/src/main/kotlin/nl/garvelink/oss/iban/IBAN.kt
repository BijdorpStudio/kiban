package nl.garvelink.oss.iban

import nl.bijdorpstudio.kiban.Iban as KibanIban // Alias for clarity
import java.util.Comparator

/**
 * Represents an International Bank Account Number (IBAN).
 * This class provides compatibility with the java-iban library API but is implemented using kiban.
 * Please migrate to nl.bijdorpstudio.kiban.Iban.
 */
@Deprecated(
    message = "This class provides compatibility with the java-iban library API but is implemented using kiban. Please migrate to nl.bijdorpstudio.kiban.Iban.",
    replaceWith = ReplaceWith("Iban", "nl.bijdorpstudio.kiban.Iban")
)
class IBAN private constructor(private val kibanIban: KibanIban) {

    @Deprecated(
        message = "This constructor is for compatibility. Migrate to using factory methods or kiban's Iban directly.",
        replaceWith = ReplaceWith("IBAN.parse(ibanString).actualKibanIban", "nl.garvelink.oss.iban.IBAN", "nl.bijdorpstudio.kiban.Iban")
    )
    constructor(ibanString: String) : this(KibanIban(ibanString))

    @Deprecated(
        message = "Migrate to kiban's Iban.toString()",
        replaceWith = ReplaceWith("actualKibanIban.toString()")
    )
    override fun toString(): String = kibanIban.toString()

    @Deprecated(
        message = "Migrate to kiban's Iban.toPlainString()",
        replaceWith = ReplaceWith("actualKibanIban.toPlainString()")
    )
    fun toPlainString(): String = kibanIban.toPlainString()

    @Deprecated(
        message = "Migrate to kiban's Iban.countryCode",
        replaceWith = ReplaceWith("actualKibanIban.countryCode")
    )
    fun getCountryCode(): String = kibanIban.countryCode

    @Deprecated(
        message = "Migrate to kiban's Iban.checkDigits",
        replaceWith = ReplaceWith("actualKibanIban.checkDigits")
    )
    fun getCheckDigits(): String = kibanIban.checkDigits

    @Deprecated(
        message = "Migrate to kiban's Iban.bban",
        replaceWith = ReplaceWith("actualKibanIban.bban")
    )
    fun getBban(): String = kibanIban.bban

    @Deprecated(
        message = "Migrate to kiban's Iban.isSEPA",
        replaceWith = ReplaceWith("actualKibanIban.isSEPA")
    )
    fun isSEPA(): Boolean = kibanIban.isSEPA

    @Deprecated(
        message = "Migrate to kiban's Iban.isInSwiftRegistry",
        replaceWith = ReplaceWith("actualKibanIban.isInSwiftRegistry")
    )
    fun isInSwiftRegistry(): Boolean = kibanIban.isInSwiftRegistry

    @Deprecated("Migration helper, do not use directly.", level = DeprecationLevel.HIDDEN)
    internal val actualKibanIban: KibanIban
        get() = kibanIban

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IBAN
        return kibanIban == other.kibanIban
    }

    override fun hashCode(): Int {
        return kibanIban.hashCode()
    }

    companion object {
        @Deprecated(
            message = "Migrate to kiban's Iban.valueOf",
            replaceWith = ReplaceWith("KibanIban.valueOf(ibanString)", "nl.bijdorpstudio.kiban.Iban as KibanIban")
        )
        @JvmStatic
        fun valueOf(ibanString: String): IBAN? {
            return KibanIban.valueOf(ibanString)?.let { IBAN(it) }
        }

        @Deprecated(
            message = "Migrate to kiban's Iban.parse",
            replaceWith = ReplaceWith("KibanIban.parse(ibanString)", "nl.bijdorpstudio.kiban.Iban as KibanIban")
        )
        @JvmStatic
        fun parse(ibanString: String): IBAN {
            return IBAN(KibanIban.parse(ibanString))
        }

        @Deprecated(
            message = "Migrate to kiban's Iban.compose",
            replaceWith = ReplaceWith("KibanIban.compose(countryCode, bban)", "nl.bijdorpstudio.kiban.Iban as KibanIban")
        )
        @JvmStatic
        fun compose(countryCode: String, bban: String): IBAN {
            return IBAN(KibanIban.compose(countryCode, bban))
        }

        @JvmField
        @Deprecated(
            message = "Migrate to kiban's Iban ordering if needed. KibanIban is Comparable.",
            replaceWith = ReplaceWith(
                "Comparator { o1, o2 -> o1?.actualKibanIban?.compareTo(o2?.actualKibanIban ?: return@Comparator if (o1 == null) 0 else -1) ?: if (o2 == null) 0 else 1 }",
                "nl.garvelink.oss.iban.IBAN", "java.util.Comparator", "nl.bijdorpstudio.kiban.Iban"
            )
        )
        val LEXICAL_ORDER: Comparator<IBAN> = Comparator { o1, o2 ->
            if (o1 == null && o2 == null) return@Comparator 0
            if (o1 == null) return@Comparator -1 // Consistent with Comparable: null is less than non-null
            if (o2 == null) return@Comparator 1  // Consistent with Comparable: non-null is greater than null
            // Delegate to kibanIban's natural order if it's Comparable, otherwise use plain string.
            // kiban.Iban is indeed Comparable<Iban>.
            o1.actualKibanIban.compareTo(o2.actualKibanIban)
        }
    }
}
