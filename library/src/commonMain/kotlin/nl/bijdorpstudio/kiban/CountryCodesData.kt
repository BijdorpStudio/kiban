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

/**
 * Contains information about IBAN country codes. This is a generated file.
 * Updated to SWIFT IBAN Registry version 97 on 2024-05-25.
 */
internal object CountryCodesData {
    /**
     * The "yyyy-MM-dd" datestamp that the embedded IBAN data was updated.
     */
    const val LAST_UPDATE_DATE = "2024-05-25"

    /**
     * The revision of the SWIFT IBAN Registry to which the embedded IBAN data was updated.
     */
    const val LAST_UPDATE_REV = "97"

    const val SEPA = 1 shl 8
    const val SWIFT = 1 shl 9
    const val REMOVE_METADATA_MASK = 0xFF
    const val BANK_IDENTIFIER_BEGIN_MASK = 0xFF
    const val BANK_IDENTIFIER_END_SHIFT = 8
    const val BANK_IDENTIFIER_END_MASK = 0xFF shl BANK_IDENTIFIER_END_SHIFT
    const val BRANCH_IDENTIFIER_BEGIN_SHIFT = 16
    const val BRANCH_IDENTIFIER_BEGIN_MASK = 0xFF shl BRANCH_IDENTIFIER_BEGIN_SHIFT
    const val BRANCH_IDENTIFIER_END_SHIFT = 24
    const val BRANCH_IDENTIFIER_END_MASK = 0xFF shl BRANCH_IDENTIFIER_END_SHIFT

    /**
     * Known country codes, this list must be sorted to allow binary search. All other lists in this file must use the
     * same indices for the same countries.
     */
    val COUNTRY_CODES: Array<String> = arrayOf(
        "AD",
        "AE",
        "AL",
        "AO",
        "AT",
        "AZ",
        "BA",
        "BE",
        "BF",
        "BG",
        "BH",
        "BI",
        "BJ",
        "BR",
        "BY",
        "CF",
        "CG",
        "CH",
        "CI",
        "CM",
        "CR",
        "CV",
        "CY",
        "CZ",
        "DE",
        "DJ",
        "DK",
        "DO",
        "DZ",
        "EE",
        "EG",
        "ES",
        "FI",
        "FK",
        "FO",
        "FR",
        "GA",
        "GB",
        "GE",
        "GI",
        "GL",
        "GQ",
        "GR",
        "GT",
        "GW",
        "HN",
        "HR",
        "HU",
        "IE",
        "IL",
        "IQ",
        "IR",
        "IS",
        "IT",
        "JO",
        "KM",
        "KW",
        "KZ",
        "LB",
        "LC",
        "LI",
        "LT",
        "LU",
        "LV",
        "LY",
        "MA",
        "MC",
        "MD",
        "ME",
        "MG",
        "MK",
        "ML",
        "MN",
        "MR",
        "MT",
        "MU",
        "MZ",
        "NE",
        "NI",
        "NL",
        "NO",
        "OM",
        "PK",
        "PL",
        "PS",
        "PT",
        "QA",
        "RO",
        "RS",
        "RU",
        "SA",
        "SC",
        "SD",
        "SE",
        "SI",
        "SK",
        "SM",
        "SN",
        "SO",
        "ST",
        "SV",
        "TD",
        "TG",
        "TL",
        "TN",
        "TR",
        "UA",
        "VA",
        "VG",
        "XK"
    )

    /**
     * Lengths for each country's IBAN. The indices match the indices of [.COUNTRY_CODES], the values are the
     * expected length. Values may embed the [.SEPA] and [.SWIFT] flags to indicate the SEPA membership and
     * whether the record is listed in the SWIFT IBAN Registry.
     */
    val COUNTRY_IBAN_LENGTHS: IntArray = intArrayOf(
        /* AD */ 24 or SWIFT or SEPA,
        /* AE */ 23 or SWIFT,
        /* AL */ 28 or SWIFT,
        /* AO */ 25,
        /* AT */ 20 or SWIFT or SEPA,
        /* AZ */ 28 or SWIFT,
        /* BA */ 20 or SWIFT,
        /* BE */ 16 or SWIFT or SEPA,
        /* BF */ 28,
        /* BG */ 22 or SWIFT or SEPA,
        /* BH */ 22 or SWIFT,
        /* BI */ 27 or SWIFT,
        /* BJ */ 28,
        /* BR */ 29 or SWIFT,
        /* BY */ 28 or SWIFT,
        /* CF */ 27,
        /* CG */ 27,
        /* CH */ 21 or SWIFT or SEPA,
        /* CI */ 28,
        /* CM */ 27,
        /* CR */ 22 or SWIFT,
        /* CV */ 25,
        /* CY */ 28 or SWIFT or SEPA,
        /* CZ */ 24 or SWIFT or SEPA,
        /* DE */ 22 or SWIFT or SEPA,
        /* DJ */ 27 or SWIFT,
        /* DK */ 18 or SWIFT or SEPA,
        /* DO */ 28 or SWIFT,
        /* DZ */ 26,
        /* EE */ 20 or SWIFT or SEPA,
        /* EG */ 29 or SWIFT,
        /* ES */ 24 or SWIFT or SEPA,
        /* FI */ 18 or SWIFT or SEPA,
        /* FK */ 18 or SWIFT,
        /* FO */ 18 or SWIFT,
        /* FR */ 27 or SWIFT or SEPA,
        /* GA */ 27,
        /* GB */ 22 or SWIFT or SEPA,
        /* GE */ 22 or SWIFT,
        /* GI */ 23 or SWIFT or SEPA,
        /* GL */ 18 or SWIFT,
        /* GQ */ 27,
        /* GR */ 27 or SWIFT or SEPA,
        /* GT */ 28 or SWIFT,
        /* GW */ 25,
        /* HN */ 28,
        /* HR */ 21 or SWIFT or SEPA,
        /* HU */ 28 or SWIFT or SEPA,
        /* IE */ 22 or SWIFT or SEPA,
        /* IL */ 23 or SWIFT,
        /* IQ */ 23 or SWIFT,
        /* IR */ 26,
        /* IS */ 26 or SWIFT or SEPA,
        /* IT */ 27 or SWIFT or SEPA,
        /* JO */ 30 or SWIFT,
        /* KM */ 27,
        /* KW */ 30 or SWIFT,
        /* KZ */ 20 or SWIFT,
        /* LB */ 28 or SWIFT,
        /* LC */ 32 or SWIFT,
        /* LI */ 21 or SWIFT or SEPA,
        /* LT */ 20 or SWIFT or SEPA,
        /* LU */ 20 or SWIFT or SEPA,
        /* LV */ 21 or SWIFT or SEPA,
        /* LY */ 25 or SWIFT,
        /* MA */ 28,
        /* MC */ 27 or SWIFT or SEPA,
        /* MD */ 24 or SWIFT,
        /* ME */ 22 or SWIFT,
        /* MG */ 27,
        /* MK */ 19 or SWIFT,
        /* ML */ 28,
        /* MN */ 20 or SWIFT,
        /* MR */ 27 or SWIFT,
        /* MT */ 31 or SWIFT or SEPA,
        /* MU */ 30 or SWIFT,
        /* MZ */ 25,
        /* NE */ 28,
        /* NI */ 28 or SWIFT,
        /* NL */ 18 or SWIFT or SEPA,
        /* NO */ 15 or SWIFT or SEPA,
        /* OM */ 23 or SWIFT,
        /* PK */ 24 or SWIFT,
        /* PL */ 28 or SWIFT or SEPA,
        /* PS */ 29 or SWIFT,
        /* PT */ 25 or SWIFT or SEPA,
        /* QA */ 29 or SWIFT,
        /* RO */ 24 or SWIFT or SEPA,
        /* RS */ 22 or SWIFT,
        /* RU */ 33 or SWIFT,
        /* SA */ 24 or SWIFT,
        /* SC */ 31 or SWIFT,
        /* SD */ 18 or SWIFT,
        /* SE */ 24 or SWIFT or SEPA,
        /* SI */ 19 or SWIFT or SEPA,
        /* SK */ 24 or SWIFT or SEPA,
        /* SM */ 27 or SWIFT or SEPA,
        /* SN */ 28,
        /* SO */ 23 or SWIFT,
        /* ST */ 25 or SWIFT,
        /* SV */ 28 or SWIFT,
        /* TD */ 27,
        /* TG */ 28,
        /* TL */ 23 or SWIFT,
        /* TN */ 24 or SWIFT,
        /* TR */ 26 or SWIFT,
        /* UA */ 29 or SWIFT,
        /* VA */ 22 or SWIFT or SEPA,
        /* VG */ 24 or SWIFT,
        /* XK */ 20 or SWIFT
    )

    /**
     * Contains the start- and end-index (as per [String.substring]) of the bank code and branch code
     * within a country's IBAN format. Mask:
     * <pre>
     * 0x000000FF <- begin offset bank id
     * 0x0000FF00 <- end offset bank id
     * 0x00FF0000 <- begin offset branch id
     * 0xFF000000 <- end offset branch id
    </pre> *
     */
    val BANK_CODE_BRANCH_CODE: IntArray = intArrayOf(
        /* AD */4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 4) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* AE */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* AL */ 4
                or ((4 + 8) shl BANK_IDENTIFIER_END_SHIFT)
                or (7 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((7 + 4) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* AO */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* AT */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* AZ */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BA */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (7 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((7 + 3) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BE */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BF */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BG */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 4) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BH */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BI */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (9 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((9 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BJ */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BR */ 4
                or ((4 + 8) shl BANK_IDENTIFIER_END_SHIFT)
                or (12 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((12 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* BY */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CF */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CG */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CH */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CI */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CM */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CR */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CV */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CY */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (7 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((7 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* CZ */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* DE */ 4
                or ((4 + 8) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* DJ */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (9 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((9 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* DK */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* DO */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* DZ */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* EE */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* EG */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 4) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* ES */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 4) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* FI */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* FK */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* FO */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* FR */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (9 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((9 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GA */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GB */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 6) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GE */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GI */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GL */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GQ */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GR */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (7 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((7 + 4) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GT */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* GW */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* HN */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* HR */ 4
                or ((4 + 7) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* HU */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (7 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((7 + 4) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* IE */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 6) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* IL */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (7 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((7 + 3) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* IQ */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 3) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* IR */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* IS */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (6 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((6 + 2) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* IT */ 5
                or ((5 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (10 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((10 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* JO */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* KM */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* KW */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* KZ */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* LB */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* LC */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* LI */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* LT */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* LU */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* LV */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* LY */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (7 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((7 + 3) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MA */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MC */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (9 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((9 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MD */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* ME */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MG */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MK */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* ML */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MN */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MR */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (9 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((9 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MT */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MU */ 4
                or ((4 + 6) shl BANK_IDENTIFIER_END_SHIFT)
                or (10 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((10 + 2) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* MZ */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* NE */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* NI */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* NL */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* NO */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* OM */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* PK */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* PL */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (4 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((4 + 8) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* PS */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* PT */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* QA */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* RO */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* RS */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* RU */ 4
                or ((4 + 9) shl BANK_IDENTIFIER_END_SHIFT)
                or (13 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((13 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SA */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SC */ 4
                or ((4 + 6) shl BANK_IDENTIFIER_END_SHIFT)
                or (10 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((10 + 2) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SD */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SE */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SI */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SK */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SM */ 5
                or ((5 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (10 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((10 + 5) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SN */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SO */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 3) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* ST */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (8 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((8 + 4) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* SV */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* TD */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* TG */ 0
                or ((0 + 0) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* TL */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* TN */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (6 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((6 + 3) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* TR */ 4
                or ((4 + 5) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* UA */ 4
                or ((4 + 6) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* VA */ 4
                or ((4 + 3) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* VG */ 4
                or ((4 + 4) shl BANK_IDENTIFIER_END_SHIFT)
                or (0 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((0 + 0) shl BRANCH_IDENTIFIER_END_SHIFT),
        /* XK */ 4
                or ((4 + 2) shl BANK_IDENTIFIER_END_SHIFT)
                or (6 shl BRANCH_IDENTIFIER_BEGIN_SHIFT)
                or ((6 + 2) shl BRANCH_IDENTIFIER_END_SHIFT)
    )
}
