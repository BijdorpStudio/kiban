/*
  Copyright 2021 Barend Garvelink, Eugen Martynov

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

import kotlin.time.Instant
import nl.bijdorpstudio.kiban.CountryCodesData.BANK_CODE_BRANCH_CODE
import nl.bijdorpstudio.kiban.CountryCodesData.BANK_IDENTIFIER_BEGIN_MASK
import nl.bijdorpstudio.kiban.CountryCodesData.BANK_IDENTIFIER_END_MASK
import nl.bijdorpstudio.kiban.CountryCodesData.BANK_IDENTIFIER_END_SHIFT
import nl.bijdorpstudio.kiban.CountryCodesData.BRANCH_IDENTIFIER_BEGIN_MASK
import nl.bijdorpstudio.kiban.CountryCodesData.BRANCH_IDENTIFIER_BEGIN_SHIFT
import nl.bijdorpstudio.kiban.CountryCodesData.BRANCH_IDENTIFIER_END_MASK
import nl.bijdorpstudio.kiban.CountryCodesData.BRANCH_IDENTIFIER_END_SHIFT
import nl.bijdorpstudio.kiban.CountryCodesData.COUNTRY_CODES
import nl.bijdorpstudio.kiban.CountryCodesData.COUNTRY_IBAN_LENGTHS
import nl.bijdorpstudio.kiban.CountryCodesData.LAST_UPDATE_DATE
import nl.bijdorpstudio.kiban.CountryCodesData.LAST_UPDATE_REV
import nl.bijdorpstudio.kiban.CountryCodesData.REMOVE_METADATA_MASK
import nl.bijdorpstudio.kiban.CountryCodesData.SEPA
import nl.bijdorpstudio.kiban.CountryCodesData.SWIFT

/**
 * The IBAN reference data this library carries: the IBAN length of each known country, its SEPA and
 * SWIFT IBAN Registry membership, and the positions of the bank and branch identifiers inside an
 * IBAN of that country.
 *
 * Every lookup here is case-sensitive. Country codes are upper case by definition, so a lower or
 * mixed case code reads as unknown rather than being normalized.
 */
public object CountryCodes {
    /** The length of the shortest IBAN among the known countries. */
    public val shortestIbanLength: Int

    /** The length of the longest IBAN among the known countries. */
    public val longestIbanLength: Int

    init {
        var min = Int.MAX_VALUE
        var max = 0
        for (countryIbanLength in COUNTRY_IBAN_LENGTHS) {
            val length = REMOVE_METADATA_MASK and countryIbanLength
            if (length > max) {
                max = length
            }
            if (length < min) {
                min = length
            }
        }
        shortestIbanLength = min
        longestIbanLength = max
    }

    /**
     * Returns the index of the given country code by binary search.
     *
     * Searches [COUNTRY_CODES] directly rather than through `asList().binarySearch(..)`: every
     * lookup on the parse path goes through here, and the list wrapper is an allocation per call
     * that buys nothing.
     *
     * @param countryCode a country code.
     * @return the array index, or the inverted insertion point (`-(insertionPoint + 1)`, always
     *   negative) if the country code is not present.
     */
    internal fun indexOf(countryCode: String): Int {
        var low = 0
        var high = COUNTRY_CODES.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val comparison = COUNTRY_CODES[mid].compareTo(countryCode)
            when {
                comparison < 0 -> low = mid + 1
                comparison > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return -(low + 1)
    }

    /**
     * Returns the bank identifier from the given plain IBAN, if available.
     *
     * Takes the already-resolved reference data index rather than looking the country up again: an
     * [Iban] resolves its index once during construction and every country-dependent property reads
     * the same row.
     *
     * @param index the index returned by [indexOf] for this IBAN's country code.
     * @param plain the IBAN value, without any spaces.
     * @return the bank ID for this IBAN, or `null` if unknown.
     */
    internal fun bankIdentifierAt(index: Int, plain: String): String? {
        if (index < 0) return null
        val data: Int = BANK_CODE_BRANCH_CODE[index]
        val bankIdBegin = data and BANK_IDENTIFIER_BEGIN_MASK
        val bankIdEnd = (data and BANK_IDENTIFIER_END_MASK) ushr BANK_IDENTIFIER_END_SHIFT
        return if (bankIdBegin != 0) plain.substring(bankIdBegin, bankIdEnd) else null
    }

    /**
     * Returns the branch identifier from the given plain IBAN, if available.
     *
     * Takes the already-resolved reference data index, for the reason given on [bankIdentifierAt].
     *
     * @param index the index returned by [indexOf] for this IBAN's country code.
     * @param plain the IBAN value, without any spaces.
     * @return the branch ID for this IBAN, or `null` if unknown.
     */
    internal fun branchIdentifierAt(index: Int, plain: String): String? {
        if (index < 0) return null
        val data: Int = BANK_CODE_BRANCH_CODE[index]
        val branchIdBegin =
            (data and BRANCH_IDENTIFIER_BEGIN_MASK) ushr BRANCH_IDENTIFIER_BEGIN_SHIFT
        val branchIdEnd = (data and BRANCH_IDENTIFIER_END_MASK) ushr BRANCH_IDENTIFIER_END_SHIFT
        return if (branchIdBegin != 0) plain.substring(branchIdBegin, branchIdEnd) else null
    }

    /**
     * Returns the IBAN length for a given country code.
     *
     * @param countryCode an upper case, two-character country code.
     * @return the IBAN length for the given country, or `null` if the input is not a known,
     *   two-character country code.
     */
    public fun ibanLength(countryCode: CharSequence): Int? {
        val index = indexOf(countryCode.toString())
        if (index > -1) {
            return COUNTRY_IBAN_LENGTHS[index] and REMOVE_METADATA_MASK
        }
        return null
    }

    /**
     * Returns whether the given country code is in SEPA.
     *
     * @param countryCode an upper case, two-character country code.
     * @return `true` if the country participates in SEPA, `false` if not or if the country code is
     *   unknown.
     */
    public fun isSepaCountry(countryCode: CharSequence): Boolean =
        isSepaCountryAt(indexOf(countryCode.toString()))

    /**
     * Returns whether the country at the given reference data index is in SEPA.
     *
     * @param index the index returned by [indexOf], negative for an unknown country code.
     * @return true if SEPA, false if not or if the index is negative.
     */
    internal fun isSepaCountryAt(index: Int): Boolean =
        index > -1 && (COUNTRY_IBAN_LENGTHS[index] and SEPA) == SEPA

    /**
     * Returns whether the source for this IBAN's format and data is the SWIFT IBAN Registry.
     *
     * @param countryCode an upper case, two-character country code.
     * @return `true` if the data for that country is from the SWIFT IBAN Registry, `false` if not
     *   or if the country code is unknown.
     */
    public fun isInSwiftRegistry(countryCode: CharSequence): Boolean =
        isInSwiftRegistryAt(indexOf(countryCode.toString()))

    /**
     * Returns whether the country at the given reference data index comes from the SWIFT IBAN
     * Registry.
     *
     * @param index the index returned by [indexOf], negative for an unknown country code.
     * @return true if our data is from the SWIFT IBAN Registry, false if not or if the index is
     *   negative.
     */
    internal fun isInSwiftRegistryAt(index: Int): Boolean =
        index > -1 && (COUNTRY_IBAN_LENGTHS[index] and SWIFT) == SWIFT

    /**
     * The known country codes, upper case, in alphabetical order.
     *
     * The list is a defensive, immutable copy of the library's reference data: it rejects every
     * mutation attempt with an [UnsupportedOperationException], including through a cast to
     * `MutableList`.
     */
    public val knownCountryCodes: List<String> = buildList { addAll(COUNTRY_CODES) }

    /**
     * Returns whether the given string is a known country code.
     *
     * @param countryCode the string to evaluate.
     * @return `true` if `countryCode` is a two-character, upper case code present in
     *   [knownCountryCodes].
     */
    public fun isKnownCountryCode(countryCode: CharSequence): Boolean {
        return countryCode.length == 2 && indexOf(countryCode.toString()) >= 0
    }

    /**
     * The date that the IBAN reference data was last updated.
     *
     * The SWIFT IBAN Registry dates its releases to the day, so the value carried here is a date,
     * not a moment. With no `LocalDate` in the standard library and a zero-dependency constraint
     * that rules out `kotlinx-datetime`, it is encoded as the [Instant] at midnight UTC on that
     * date. That encoding is part of the contract: the returned instant always has a zero
     * time-of-day component and renders as `yyyy-mm-ddT00:00:00Z`. Read the date off it, not the
     * time of day, and do not read a local calendar date off it in a non-UTC zone.
     *
     * [Instant] is safe to depend on here: it is a stable, non-experimental standard library type
     * from Kotlin 2.3 onwards, which is also this library's minimum supported Kotlin version. See
     * `docs/instant-api-stability.md` for the analysis behind freezing it into the API.
     *
     * Parsed once, when this object initializes: the encoded date is a compile-time constant, so
     * re-parsing it on every read would buy nothing.
     */
    public val lastUpdateDate: Instant = Instant.parse("${LAST_UPDATE_DATE}T00:00:00Z")

    /** The revision of the SWIFT IBAN Registry the reference data was taken from. */
    public const val LAST_UPDATE_REVISION: String = LAST_UPDATE_REV
}
