package nl.garvelink.oss.iban

import nl.bijdorpstudio.kiban.Iban
import java.util.Comparator

/**
 * This class provides compatibility with the java-iban library API but is implemented using kiban.
 * Please migrate to nl.bijdorpstudio.kiban.Iban.
 */
@Deprecated(
    message = "This class provides compatibility with the java-iban library API but is implemented using kiban. Please migrate to nl.bijdorpstudio.kiban.Iban.",
    replaceWith = ReplaceWith("Iban", "nl.bijdorpstudio.kiban.Iban")
)
class IBAN private constructor(internal val iban: Iban) {

    override fun toString(): String = iban.toString()

    fun toPlainString(): String = iban.toPlainString()

    fun getCountryCode(): String = iban.countryCode

    fun getCheckDigits(): String = iban.checkDigits

    fun isSEPA(): Boolean = iban.isSEPA

    fun isInSwiftRegistry(): Boolean = iban.isInSwiftRegistry

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IBAN
        return iban == other.iban
    }

    override fun hashCode(): Int = iban.hashCode()

    companion object {
        @Deprecated(
            message = "Migrate to kiban's Iban.valueOf",
            replaceWith = ReplaceWith("Iban.valueOf(ibanString)", "nl.bijdorpstudio.kiban.Iban as Iban")
        )
        @JvmStatic
        fun valueOf(ibanString: String): IBAN? = Iban.valueOf(ibanString)?.let { IBAN(it) }

        @Deprecated(
            message = "Migrate to kiban's Iban.parse",
            replaceWith = ReplaceWith("Iban.parse(ibanString)", "nl.bijdorpstudio.kiban.Iban as Iban")
        )
        @JvmStatic
        fun parse(ibanString: String): IBAN = IBAN(Iban.parse(ibanString))

        @Deprecated(
            message = "Migrate to kiban's Iban.compose",
            replaceWith = ReplaceWith("Iban.compose(countryCode, bban)", "nl.bijdorpstudio.kiban.Iban as Iban")
        )
        @JvmStatic
        fun compose(countryCode: String, bban: String): IBAN = IBAN(Iban.compose(countryCode, bban))

        @JvmField
        @Deprecated(
            message = "Migrate to kiban's Iban ordering if needed. Iban is Comparable.", replaceWith = ReplaceWith(
                "Comparator { o1, o2 -> o1?.actualIban?.compareTo(o2?.actualIban ?: return@Comparator if (o1 == null) 0 else -1) ?: if (o2 == null) 0 else 1 }",
                "nl.garvelink.oss.iban.IBAN",
                "java.util.Comparator",
                "nl.bijdorpstudio.kiban.Iban"
            )
        )
        val LEXICAL_ORDER: Comparator<IBAN> = Comparator { o1, o2 ->
            if (o1 == null && o2 == null) return@Comparator 0
            if (o1 == null) return@Comparator -1 // Consistent with Comparable: null is less than non-null
            if (o2 == null) return@Comparator 1  // Consistent with Comparable: non-null is greater than null
            // Delegate to kibanIban's natural order if it's Comparable, otherwise use plain string.
            // kiban.Iban is indeed Comparable<Iban>.
            o1.iban.compareTo(o2.iban)
        }
    }
}
