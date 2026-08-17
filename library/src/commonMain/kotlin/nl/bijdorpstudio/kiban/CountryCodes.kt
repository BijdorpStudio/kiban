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

/** Contains information about IBAN country codes. */
object CountryCodes {
    /** The length of the shortest IBAN among the known countries. */
    val shortestIbanLength: Int

    /** The length of the longest IBAN among the known countries. */
    val longestIbanLength: Int

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
     * @param countryCode a country code.
     * @return the array index, or -1.
     */
    internal fun indexOf(countryCode: String): Int =
        COUNTRY_CODES.asList().binarySearch(countryCode)

    /**
     * Returns the bank identifier from the given IBAN, if available.
     *
     * @param iban an iban to evaluate. Cannot be null.
     * @return the bank ID for this IBAN, or `null` if unknown.
     */
    internal fun getBankIdentifier(iban: Iban): String? {
        val index: Int = indexOf(iban.countryCode)
        if (index > -1) {
            val data: Int = BANK_CODE_BRANCH_CODE[index]
            val bankIdBegin = data and BANK_IDENTIFIER_BEGIN_MASK
            val bankIdEnd = (data and BANK_IDENTIFIER_END_MASK) ushr BANK_IDENTIFIER_END_SHIFT
            return if (bankIdBegin != 0) iban.plain.substring(bankIdBegin, bankIdEnd) else null
        }
        return null
    }

    /**
     * Returns the branch identifier from the given IBAN, if available.
     *
     * @param iban an iban to evaluate. Cannot be null.
     * @return the branch ID for this IBAN, or `null` if unknown.
     */
    internal fun getBranchIdentifier(iban: Iban): String? {
        val index: Int = indexOf(iban.countryCode)
        if (index > -1) {
            val data: Int = BANK_CODE_BRANCH_CODE[index]
            val branchIdBegin =
                (data and BRANCH_IDENTIFIER_BEGIN_MASK) ushr BRANCH_IDENTIFIER_BEGIN_SHIFT
            val branchIdEnd = (data and BRANCH_IDENTIFIER_END_MASK) ushr BRANCH_IDENTIFIER_END_SHIFT
            return if (branchIdBegin != 0) iban.plain.substring(branchIdBegin, branchIdEnd)
            else null
        }
        return null
    }

    /**
     * Returns the IBAN length for a given country code.
     *
     * @param countryCode a non-null, uppercase, two-character country code.
     * @return the IBAN length for the given country, or null if the input is not a known,
     *   two-character country code.
     */
    fun ibanLength(countryCode: CharSequence): Int? {
        val index = indexOf(countryCode.toString())
        if (index > -1) {
            return COUNTRY_IBAN_LENGTHS[index] and REMOVE_METADATA_MASK
        }
        return null
    }

    /**
     * Returns whether the given country code is in SEPA.
     *
     * @param countryCode a non-null, uppercase, two-character country code.
     * @return true if SEPA, false if not.
     */
    fun isSEPACountry(countryCode: CharSequence): Boolean {
        val index = indexOf(countryCode.toString())
        return index > -1 && (COUNTRY_IBAN_LENGTHS[index] and SEPA) == SEPA
    }

    /**
     * Returns whether the source for this IBAN's format and data is the SWIFT IBAN Registry.
     *
     * @param countryCode a non-null, uppercase, two-character country code.
     * @return true if our data is from the SWIFT IBAN Registry, false if not.
     */
    fun isInSwiftRegistry(countryCode: CharSequence): Boolean {
        val index = indexOf(countryCode.toString())
        return index > -1 && (COUNTRY_IBAN_LENGTHS[index] and SWIFT) == SWIFT
    }

    /**
     * The known country codes, upper case, in alphabetical order.
     *
     * The list is a defensive, immutable copy of the library's reference data: it rejects every
     * mutation attempt with an [UnsupportedOperationException], including through a cast to
     * `MutableList`.
     */
    val knownCountryCodes: List<String> = buildList { addAll(COUNTRY_CODES) }

    /**
     * Returns whether the given string is a known country code.
     *
     * @param countryCode the string to evaluate.
     * @return `true` if `aCountryCode` is a two-letter, uppercase String present in
     *   [knownCountryCodes].
     */
    fun isKnownCountryCode(countryCode: CharSequence): Boolean {
        return countryCode.length == 2 && indexOf(countryCode.toString()) >= 0
    }

    /**
     * Returns the date that the IBAN reference data was last updated.
     *
     * @return the last update date of the reference data in this library.
     */
    val lastUpdateDate: Instant
        get() = Instant.parse("${LAST_UPDATE_DATE}T00:00:00Z")

    /**
     * Returns the version information of the SWIFT IBAN Registry used on [.lastUpdateDate].
     *
     * @return revision information of the SWIFT IBAN Registry.
     */
    const val lastUpdateRevision: String = LAST_UPDATE_REV
}
