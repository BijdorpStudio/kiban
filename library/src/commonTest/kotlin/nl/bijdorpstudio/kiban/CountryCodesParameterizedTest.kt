/*
   Copyright 2023 Barend Garvelink, Eugen Martynov

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

import assertk.Table1
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.tableOf
import kotlin.test.Test

/**
 * Ensures that the [Iban] class accepts IBAN numbers from every participating country
 * (...known at the time the test was last updated).
 */
class CountryCodesParameterizedTest {

    @Test
    fun `Length for country code should return correct value`() {
        countriesTestDataTable
            .forAll { testData ->
                val lengthForCountryCode = CountryCodes.getLengthForCountryCode(testData.plain.substring(0, 2))
                assertThat(lengthForCountryCode).isEqualTo(testData.plain.length)
            }
    }

    @Test
    fun `Is known country code should return true`() {
        countriesTestDataTable
            .forAll { td ->
                assertThat(CountryCodes.isKnownCountryCode(td.plain.substring(0, 2))).isTrue()
            }
    }

    @Test
    fun `All country codes should be tested`() {
        val testDataCountryCodes = testData
            .map { it.plain.substring(0, 2) }
            .toSet()

        assertThat(CountryCodes.knownCountryCodes.toSet() - testDataCountryCodes)
            .isEmpty()
    }

    companion object {
        /**
         * List of valid international IBAN's.
         * References:
         * - SWIFT: https://www.swift.com/resource/iban-registry-pdf
         * - IBAN.com Experimental List: https://www.iban.com/structure
         */
        val testData = listOf(
            // Countries in the SWIFT reference, sorted by Country Code
            IbanCountryTestData(
                name = "Andorra",
                swift = true,
                sepa = true,
                plain = "AD1200012030200359100100",
                bank = "0001",
                branch = "2030",
                pretty = "AD12 0001 2030 2003 5910 0100"
            ),
            IbanCountryTestData(
                name = "United Arab Emirates (The)",
                swift = true,
                sepa = false,
                plain = "AE070331234567890123456",
                bank = "033",
                branch = null,
                pretty = "AE07 0331 2345 6789 0123 456"
            ),
            IbanCountryTestData(
                name = "Albania",
                swift = true,
                sepa = false,
                plain = "AL47212110090000000235698741",
                bank = "21211009",
                branch = "1100",
                pretty = "AL47 2121 1009 0000 0002 3569 8741"
            ),
            IbanCountryTestData(
                name = "Austria",
                swift = true,
                sepa = true,
                plain = "AT611904300234573201",
                bank = "19043",
                branch = null,
                pretty = "AT61 1904 3002 3457 3201"
            ),
            IbanCountryTestData(
                name = "Azerbaijan",
                swift = true,
                sepa = false,
                plain = "AZ21NABZ00000000137010001944",
                bank = "NABZ",
                branch = null,
                pretty = "AZ21 NABZ 0000 0000 1370 1000 1944"
            ),
            IbanCountryTestData(
                name = "Bosnia and Herzegovinia",
                swift = true,
                sepa = false,
                plain = "BA391290079401028494",
                bank = "129",
                branch = "007",
                pretty = "BA39 1290 0794 0102 8494"
            ),
            IbanCountryTestData(
                name = "Belgium",
                swift = true,
                sepa = true,
                plain = "BE68539007547034",
                bank = "539",
                branch = null,
                pretty = "BE68 5390 0754 7034"
            ),
            IbanCountryTestData(
                name = "Bulgaria",
                swift = true,
                sepa = true,
                plain = "BG80BNBG96611020345678",
                bank = "BNBG",
                branch = "9661",
                pretty = "BG80 BNBG 9661 1020 3456 78"
            ),
            IbanCountryTestData(
                name = "Bahrain",
                swift = true,
                sepa = false,
                plain = "BH67BMAG00001299123456",
                bank = "BMAG",
                branch = null,
                pretty = "BH67 BMAG 0000 1299 1234 56"
            ),
            IbanCountryTestData(
                name = "Burundi",
                swift = true,
                sepa = false,
                plain = "BI4210000100010000332045181",
                bank = "10000",
                branch = "10001",
                pretty = "BI42 1000 0100 0100 0033 2045 181"
            ),
            IbanCountryTestData(
                name = "Brazil",
                swift = true,
                sepa = false,
                plain = "BR1800360305000010009795493C1",
                bank = "00360305",
                branch = "00001",
                pretty = "BR18 0036 0305 0000 1000 9795 493C 1"
            ),
            IbanCountryTestData(
                name = "Republic of Belarus",
                swift = true,
                sepa = false,
                plain = "BY13NBRB3600900000002Z00AB00",
                bank = "NBRB",
                branch = null,
                pretty = "BY13 NBRB 3600 9000 0000 2Z00 AB00"
            ),
            IbanCountryTestData(
                name = "Switzerland",
                swift = true,
                sepa = true,
                plain = "CH9300762011623852957",
                bank = "00762",
                branch = null,
                pretty = "CH93 0076 2011 6238 5295 7"
            ),
            IbanCountryTestData(
                name = "Costa Rica",
                swift = true,
                sepa = false,
                plain = "CR05015202001026284066",
                bank = "0152",
                branch = null,
                pretty = "CR05 0152 0200 1026 2840 66"
            ),
            IbanCountryTestData(
                name = "Cyprus",
                swift = true,
                sepa = true,
                plain = "CY17002001280000001200527600",
                bank = "002",
                branch = "00128",
                pretty = "CY17 0020 0128 0000 0012 0052 7600"
            ),
            IbanCountryTestData(
                name = "Czech Republic",
                swift = true,
                sepa = true,
                plain = "CZ6508000000192000145399",
                bank = "0800",
                branch = null,
                pretty = "CZ65 0800 0000 1920 0014 5399"
            ),
            IbanCountryTestData(
                name = "Germany",
                swift = true,
                sepa = true,
                plain = "DE89370400440532013000",
                bank = "37040044",
                branch = null,
                pretty = "DE89 3704 0044 0532 0130 00"
            ),
            IbanCountryTestData(
                name = "Djibouti",
                swift = true,
                sepa = false,
                plain = "DJ2100010000000154000100186",
                bank = "00010",
                branch = "00000",
                pretty = "DJ21 0001 0000 0001 5400 0100 186"
            ),
            IbanCountryTestData(
                name = "Denmark",
                swift = true,
                sepa = true,
                plain = "DK5000400440116243",
                bank = "0040",
                branch = null,
                pretty = "DK50 0040 0440 1162 43"
            ),
            IbanCountryTestData(
                name = "Dominican Republic",
                swift = true,
                sepa = false,
                plain = "DO28BAGR00000001212453611324",
                bank = "BAGR",
                branch = null,
                pretty = "DO28 BAGR 0000 0001 2124 5361 1324"
            ),
            IbanCountryTestData(
                name = "Estonia",
                swift = true,
                sepa = true,
                plain = "EE382200221020145685",
                bank = "22",
                branch = null,
                pretty = "EE38 2200 2210 2014 5685"
            ),
            IbanCountryTestData(
                name = "Egypt",
                swift = true,
                sepa = false,
                plain = "EG380019000500000000263180002",
                bank = "0019",
                branch = "0005",
                pretty = "EG38 0019 0005 0000 0000 2631 8000 2"
            ),
            IbanCountryTestData(
                name = "Spain",
                swift = true,
                sepa = true,
                plain = "ES9121000418450200051332",
                bank = "2100",
                branch = "0418",
                pretty = "ES91 2100 0418 4502 0005 1332"
            ),
            IbanCountryTestData(
                name = "Finland",
                swift = true,
                sepa = true,
                plain = "FI2112345600000785",
                bank = "123",
                branch = null,
                pretty = "FI21 1234 5600 0007 85"
            ),
            IbanCountryTestData(
                name = "Falkland Islands",
                swift = true,
                sepa = false,
                plain = "FK88SC123456789012",
                bank = "SC",
                branch = null,
                pretty = "FK88 SC12 3456 7890 12"
            ),
            IbanCountryTestData(
                name = "Faroe Islands",
                swift = true,
                sepa = false,
                plain = "FO6264600001631634",
                bank = "6460",
                branch = null,
                pretty = "FO62 6460 0001 6316 34"
            ),
            IbanCountryTestData(
                name = "France",
                swift = true,
                sepa = true,
                plain = "FR1420041010050500013M02606",
                bank = "20041",
                branch = "01005",
                pretty = "FR14 2004 1010 0505 0001 3M02 606"
            ),
            IbanCountryTestData(
                name = "United Kingdom",
                swift = true,
                sepa = true,
                plain = "GB29NWBK60161331926819",
                bank = "NWBK",
                branch = "601613",
                pretty = "GB29 NWBK 6016 1331 9268 19"
            ),
            IbanCountryTestData(
                name = "Georgia",
                swift = true,
                sepa = false,
                plain = "GE29NB0000000101904917",
                bank = "NB",
                branch = null,
                pretty = "GE29 NB00 0000 0101 9049 17"
            ),
            IbanCountryTestData(
                name = "Gibraltar",
                swift = true,
                sepa = true,
                plain = "GI75NWBK000000007099453",
                bank = "NWBK",
                branch = null,
                pretty = "GI75 NWBK 0000 0000 7099 453"
            ),
            IbanCountryTestData(
                name = "Greenland",
                swift = true,
                sepa = false,
                plain = "GL8964710001000206",
                bank = "6471",
                branch = null,
                pretty = "GL89 6471 0001 0002 06"
            ),
            IbanCountryTestData(
                name = "Greece",
                swift = true,
                sepa = true,
                plain = "GR1601101250000000012300695",
                bank = "011",
                branch = "0125",
                pretty = "GR16 0110 1250 0000 0001 2300 695"
            ),
            IbanCountryTestData(
                name = "Guatemala",
                swift = true,
                sepa = false,
                plain = "GT82TRAJ01020000001210029690",
                bank = "TRAJ",
                branch = null,
                pretty = "GT82 TRAJ 0102 0000 0012 1002 9690"
            ),
            IbanCountryTestData(
                name = "Croatia",
                swift = true,
                sepa = true,
                plain = "HR1210010051863000160",
                bank = "1001005",
                branch = null,
                pretty = "HR12 1001 0051 8630 0016 0"
            ),
            IbanCountryTestData(
                name = "Hungary",
                swift = true,
                sepa = true,
                plain = "HU42117730161111101800000000",
                bank = "117",
                branch = "7301",
                pretty = "HU42 1177 3016 1111 1018 0000 0000"
            ),
            IbanCountryTestData(
                name = "Ireland",
                swift = true,
                sepa = true,
                plain = "IE29AIBK93115212345678",
                bank = "AIBK",
                branch = "931152",
                pretty = "IE29 AIBK 9311 5212 3456 78"
            ),
            IbanCountryTestData(
                name = "Israel",
                swift = true,
                sepa = false,
                plain = "IL620108000000099999999",
                bank = "010",
                branch = "800",
                pretty = "IL62 0108 0000 0009 9999 999"
            ),
            IbanCountryTestData(
                name = "Iraq",
                swift = true,
                sepa = false,
                plain = "IQ98NBIQ850123456789012",
                bank = "NBIQ",
                branch = "850",
                pretty = "IQ98 NBIQ 8501 2345 6789 012"
            ),
            IbanCountryTestData(
                name = "Iceland",
                swift = true,
                sepa = true,
                plain = "IS140159260076545510730339",
                bank = "01",
                branch = "59",
                pretty = "IS14 0159 2600 7654 5510 7303 39"
            ),
            IbanCountryTestData(
                name = "Italy",
                swift = true,
                sepa = true,
                plain = "IT60X0542811101000000123456",
                bank = "05428",
                branch = "11101",
                pretty = "IT60 X054 2811 1010 0000 0123 456"
            ),
            IbanCountryTestData(
                name = "Jordan",
                swift = true,
                sepa = false,
                plain = "JO94CBJO0010000000000131000302",
                bank = "CBJO",
                branch = null,
                pretty = "JO94 CBJO 0010 0000 0000 0131 0003 02"
            ),
            IbanCountryTestData(
                name = "Kuwait",
                swift = true,
                sepa = false,
                plain = "KW81CBKU0000000000001234560101",
                bank = "CBKU",
                branch = null,
                pretty = "KW81 CBKU 0000 0000 0000 1234 5601 01"
            ),
            IbanCountryTestData(
                name = "Kazakhstan",
                swift = true,
                sepa = false,
                plain = "KZ86125KZT5004100100",
                bank = "125",
                branch = null,
                pretty = "KZ86 125K ZT50 0410 0100"
            ),
            IbanCountryTestData(
                name = "Lebanon",
                swift = true,
                sepa = false,
                plain = "LB62099900000001001901229114",
                bank = "0999",
                branch = null,
                pretty = "LB62 0999 0000 0001 0019 0122 9114"
            ),
            IbanCountryTestData(
                name = "Saint Lucia",
                swift = true,
                sepa = false,
                plain = "LC55HEMM000100010012001200023015",
                bank = "HEMM",
                branch = null,
                pretty = "LC55 HEMM 0001 0001 0012 0012 0002 3015"
            ),
            IbanCountryTestData(
                name = "Liechtenstein",
                swift = true,
                sepa = true,
                plain = "LI21088100002324013AA",
                bank = "08810",
                branch = null,
                pretty = "LI21 0881 0000 2324 013A A"
            ),
            IbanCountryTestData(
                name = "Lithuania",
                swift = true,
                sepa = true,
                plain = "LT121000011101001000",
                bank = "10000",
                branch = null,
                pretty = "LT12 1000 0111 0100 1000"
            ),
            IbanCountryTestData(
                name = "Luxembourg",
                swift = true,
                sepa = true,
                plain = "LU280019400644750000",
                bank = "001",
                branch = null,
                pretty = "LU28 0019 4006 4475 0000"
            ),
            IbanCountryTestData(
                name = "Latvia",
                swift = true,
                sepa = true,
                plain = "LV80BANK0000435195001",
                bank = "BANK",
                branch = null,
                pretty = "LV80 BANK 0000 4351 9500 1"
            ),
            IbanCountryTestData(
                name = "Libya",
                swift = true,
                sepa = false,
                plain = "LY83002048000020100120361",
                bank = "002",
                branch = "048",
                pretty = "LY83 0020 4800 0020 1001 2036 1"
            ),
            IbanCountryTestData(
                name = "Monaco",
                swift = true,
                sepa = true,
                plain = "MC5811222000010123456789030",
                bank = "11222",
                branch = "00001",
                pretty = "MC58 1122 2000 0101 2345 6789 030"
            ),
            IbanCountryTestData(
                name = "Moldova",
                swift = true,
                sepa = false,
                plain = "MD24AG000225100013104168",
                bank = "AG",
                branch = null,
                pretty = "MD24 AG00 0225 1000 1310 4168"
            ),
            IbanCountryTestData(
                name = "Montenegro",
                swift = true,
                sepa = false,
                plain = "ME25505000012345678951",
                bank = "505",
                branch = null,
                pretty = "ME25 5050 0001 2345 6789 51"
            ),
            IbanCountryTestData(
                name = "Macedonia",
                swift = true,
                sepa = false,
                plain = "MK07250120000058984",
                bank = "250",
                branch = null,
                pretty = "MK07 2501 2000 0058 984"
            ),
            IbanCountryTestData(
                name = "Mongolia",
                swift = true,
                sepa = false,
                plain = "MN121234123456789123",
                bank = "1234",
                branch = null,
                pretty = "MN12 1234 1234 5678 9123"
            ),
            IbanCountryTestData(
                name = "Mauritania",
                swift = true,
                sepa = false,
                plain = "MR1300020001010000123456753",
                bank = "00020",
                branch = "00101",
                pretty = "MR13 0002 0001 0100 0012 3456 753"
            ),
            IbanCountryTestData(
                name = "Malta",
                swift = true,
                sepa = true,
                plain = "MT84MALT011000012345MTLCAST001S",
                bank = "MALT",
                branch = "01100",
                pretty = "MT84 MALT 0110 0001 2345 MTLC AST0 01S"
            ),
            IbanCountryTestData(
                name = "Mauritius",
                swift = true,
                sepa = false,
                plain = "MU17BOMM0101101030300200000MUR",
                bank = "BOMM01",
                branch = "01",
                pretty = "MU17 BOMM 0101 1010 3030 0200 000M UR"
            ),
            IbanCountryTestData(
                name = "Nicaragua",
                swift = true,
                sepa = false,
                plain = "NI45BAPR00000013000003558124",
                bank = "BAPR",
                branch = null,
                pretty = "NI45 BAPR 0000 0013 0000 0355 8124"
            ),
            IbanCountryTestData(
                name = "Netherlands (The)",
                swift = true,
                sepa = true,
                plain = "NL91ABNA0417164300",
                bank = "ABNA",
                branch = null,
                pretty = "NL91 ABNA 0417 1643 00"
            ),
            IbanCountryTestData(
                name = "Norway",
                swift = true,
                sepa = true,
                plain = "NO9386011117947",
                bank = "8601",
                branch = null,
                pretty = "NO93 8601 1117 947"
            ),
            IbanCountryTestData(
                name = "Oman",
                swift = true,
                sepa = false,
                plain = "OM810180000001299123456",
                bank = "018",
                branch = null,
                pretty = "OM81 0180 0000 0129 9123 456"
            ),
            IbanCountryTestData(
                name = "Pakistan",
                swift = true,
                sepa = false,
                plain = "PK36SCBL0000001123456702",
                bank = "SCBL",
                branch = null,
                pretty = "PK36 SCBL 0000 0011 2345 6702"
            ),
            IbanCountryTestData(
                name = "Poland",
                swift = true,
                sepa = true,
                plain = "PL61109010140000071219812874",
                bank = null,
                branch = "10901014",
                pretty = "PL61 1090 1014 0000 0712 1981 2874"
            ),
            IbanCountryTestData(
                name = "Palestine, State of",
                swift = true,
                sepa = false,
                plain = "PS92PALS000000000400123456702",
                bank = "PALS",
                branch = null,
                pretty = "PS92 PALS 0000 0000 0400 1234 5670 2"
            ),
            IbanCountryTestData(
                name = "Portugal",
                swift = true,
                sepa = true,
                plain = "PT50000201231234567890154",
                bank = "0002",
                branch = null,
                pretty = "PT50 0002 0123 1234 5678 9015 4"
            ),
            IbanCountryTestData(
                name = "Qatar",
                swift = true,
                sepa = false,
                plain = "QA58DOHB00001234567890ABCDEFG",
                bank = "DOHB",
                branch = null,
                pretty = "QA58 DOHB 0000 1234 5678 90AB CDEF G"
            ),
            IbanCountryTestData(
                name = "Romania",
                swift = true,
                sepa = true,
                plain = "RO49AAAA1B31007593840000",
                bank = "AAAA",
                branch = null,
                pretty = "RO49 AAAA 1B31 0075 9384 0000"
            ),
            IbanCountryTestData(
                name = "Serbia",
                swift = true,
                sepa = false,
                plain = "RS35260005601001611379",
                bank = "260",
                branch = null,
                pretty = "RS35 2600 0560 1001 6113 79"
            ),
            IbanCountryTestData(
                name = "Russia",
                swift = true,
                sepa = false,
                plain = "RU0304452522540817810538091310419",
                bank = "044525225",
                branch = "40817",
                pretty = "RU03 0445 2522 5408 1781 0538 0913 1041 9"
            ),
            IbanCountryTestData(
                name = "Saudi Arabia",
                swift = true,
                sepa = false,
                plain = "SA0380000000608010167519",
                bank = "80",
                branch = null,
                pretty = "SA03 8000 0000 6080 1016 7519"
            ),
            IbanCountryTestData(
                name = "Seychelles",
                swift = true,
                sepa = false,
                plain = "SC18SSCB11010000000000001497USD",
                bank = "SSCB11",
                branch = "01",
                pretty = "SC18 SSCB 1101 0000 0000 0000 1497 USD"
            ),
            IbanCountryTestData(
                name = "Sudan",
                swift = true,
                sepa = false,
                plain = "SD2129010501234001",
                bank = "29",
                branch = null,
                pretty = "SD21 2901 0501 2340 01"
            ),
            IbanCountryTestData(
                name = "Sweden",
                swift = true,
                sepa = true,
                plain = "SE4550000000058398257466",
                bank = "500",
                branch = null,
                pretty = "SE45 5000 0000 0583 9825 7466"
            ),
            IbanCountryTestData(
                name = "Slovenia",
                swift = true,
                sepa = true,
                plain = "SI56263300012039086",
                bank = "26330",
                branch = null,
                pretty = "SI56 2633 0001 2039 086"
            ),
            IbanCountryTestData(
                name = "Slovakia",
                swift = true,
                sepa = true,
                plain = "SK3112000000198742637541",
                bank = "1200",
                branch = null,
                pretty = "SK31 1200 0000 1987 4263 7541"
            ),
            IbanCountryTestData(
                name = "San Marino",
                swift = true,
                sepa = true,
                plain = "SM86U0322509800000000270100",
                bank = "03225",
                branch = "09800",
                pretty = "SM86 U032 2509 8000 0000 0270 100"
            ),
            IbanCountryTestData(
                name = "Somalia",
                swift = true,
                sepa = false,
                plain = "SO211000001001000100141",
                bank = "1000",
                branch = "001",
                pretty = "SO21 1000 0010 0100 0100 141"
            ),
            IbanCountryTestData(
                name = "Sao Tome e Principe",
                swift = true,
                sepa = false,
                plain = "ST23000100010051845310146",
                bank = "0001",
                branch = "0001",
                pretty = "ST23 0001 0001 0051 8453 1014 6"
            ),
            IbanCountryTestData(
                name = "El Salvador",
                swift = true,
                sepa = false,
                plain = "SV62CENR00000000000000700025",
                bank = "CENR",
                branch = null,
                pretty = "SV62 CENR 0000 0000 0000 0070 0025"
            ),
            IbanCountryTestData(
                name = "Timor-Leste",
                swift = true,
                sepa = false,
                plain = "TL380080012345678910157",
                bank = "008",
                branch = null,
                pretty = "TL38 0080 0123 4567 8910 157"
            ),
            IbanCountryTestData(
                name = "Tunisia",
                swift = true,
                sepa = false,
                plain = "TN5910006035183598478831",
                bank = "10",
                branch = "006",
                pretty = "TN59 1000 6035 1835 9847 8831"
            ),
            IbanCountryTestData(
                name = "Turkey",
                swift = true,
                sepa = false,
                plain = "TR330006100519786457841326",
                bank = "00061",
                branch = null,
                pretty = "TR33 0006 1005 1978 6457 8413 26"
            ),
            IbanCountryTestData(
                name = "Ukraine",
                swift = true,
                sepa = false,
                plain = "UA213223130000026007233566001",
                bank = "322313",
                branch = null,
                pretty = "UA21 3223 1300 0002 6007 2335 6600 1"
            ),
            IbanCountryTestData(
                name = "Vatican City State",
                swift = true,
                sepa = true,
                plain = "VA59001123000012345678",
                bank = "001",
                branch = null,
                pretty = "VA59 0011 2300 0012 3456 78"
            ),
            IbanCountryTestData(
                name = "Virgin Islands",
                swift = true,
                sepa = false,
                plain = "VG96VPVG0000012345678901",
                bank = "VPVG",
                branch = null,
                pretty = "VG96 VPVG 0000 0123 4567 8901"
            ),
            IbanCountryTestData(
                name = "Kosovo",
                swift = true,
                sepa = false,
                plain = "XK051212012345678906",
                bank = "12",
                branch = "12",
                pretty = "XK05 1212 0123 4567 8906"
            ),
            // Countries in the IBAN.com Experimental List, sorted by Name
            IbanCountryTestData(
                name = "Algeria",
                swift = false,
                sepa = false,
                plain = "DZ580002100001113000000570",
                bank = null,
                branch = null,
                pretty = "DZ58 0002 1000 0111 3000 0005 70"
            ),
            IbanCountryTestData(
                name = "Angola",
                swift = false,
                sepa = false,
                plain = "AO06004400006729503010102",
                bank = null,
                branch = null,
                pretty = "AO06 0044 0000 6729 5030 1010 2"
            ),
            IbanCountryTestData(
                name = "Benin",
                swift = false,
                sepa = false,
                plain = "BJ66BJ0610100100144390000769",
                bank = null,
                branch = null,
                pretty = "BJ66 BJ06 1010 0100 1443 9000 0769"
            ),
            IbanCountryTestData(
                name = "Burkina Faso",
                swift = false,
                sepa = false,
                plain = "BF42BF0840101300463574000390",
                bank = null,
                branch = null,
                pretty = "BF42 BF08 4010 1300 4635 7400 0390"
            ),
            IbanCountryTestData(
                name = "Cameroon",
                swift = false,
                sepa = false,
                plain = "CM2110002000300277976315008",
                bank = null,
                branch = null,
                pretty = "CM21 1000 2000 3002 7797 6315 008"
            ),
            IbanCountryTestData(
                name = "Cape Verde",
                swift = false,
                sepa = false,
                plain = "CV64000500000020108215144",
                bank = null,
                branch = null,
                pretty = "CV64 0005 0000 0020 1082 1514 4"
            ),
            IbanCountryTestData(
                name = "Central African Republic",
                swift = false,
                sepa = false,
                plain = "CF4220001000010120069700160",
                bank = null,
                branch = null,
                pretty = "CF42 2000 1000 0101 2006 9700 160"
            ),
            IbanCountryTestData(
                name = "Chad",
                swift = false,
                sepa = false,
                plain = "TD8960002000010271091600153",
                bank = null,
                branch = null,
                pretty = "TD89 6000 2000 0102 7109 1600 153"
            ),
            IbanCountryTestData(
                name = "Comoros",
                swift = false,
                sepa = false,
                plain = "KM4600005000010010904400137",
                bank = null,
                branch = null,
                pretty = "KM46 0000 5000 0100 1090 4400 137"
            ),
            IbanCountryTestData(
                name = "Congo",
                swift = false,
                sepa = false,
                plain = "CG3930011000101013451300019",
                bank = null,
                branch = null,
                pretty = "CG39 3001 1000 1010 1345 1300 019"
            ),
            IbanCountryTestData(
                name = "Equatorial Guinea",
                swift = false,
                sepa = false,
                plain = "GQ7050002001003715228190196",
                bank = null,
                branch = null,
                pretty = "GQ70 5000 2001 0037 1522 8190 196"
            ),
            IbanCountryTestData(
                name = "Gabon",
                swift = false,
                sepa = false,
                plain = "GA2140021010032001890020126",
                bank = null,
                branch = null,
                pretty = "GA21 4002 1010 0320 0189 0020 126"
            ),
            IbanCountryTestData(
                name = "Guinea-Bissau",
                swift = false,
                sepa = false,
                plain = "GW04GW1430010181800637601",
                bank = null,
                branch = null,
                pretty = "GW04 GW14 3001 0181 8006 3760 1"
            ),
            IbanCountryTestData(
                name = "Honduras",
                swift = false,
                sepa = false,
                plain = "HN54PISA00000000000000123124",
                bank = null,
                branch = null,
                pretty = "HN54 PISA 0000 0000 0000 0012 3124"
            ),
            IbanCountryTestData(
                name = "Iran",
                swift = false,
                sepa = false,
                plain = "IR710570029971601460641001",
                bank = null,
                branch = null,
                pretty = "IR71 0570 0299 7160 1460 6410 01"
            ),
            IbanCountryTestData(
                name = "Ivory Coast",
                swift = false,
                sepa = false,
                plain = "CI93CI0080111301134291200589",
                bank = null,
                branch = null,
                pretty = "CI93 CI00 8011 1301 1342 9120 0589"
            ),
            IbanCountryTestData(
                name = "Madagascar",
                swift = false,
                sepa = false,
                plain = "MG4600005030071289421016045",
                bank = null,
                branch = null,
                pretty = "MG46 0000 5030 0712 8942 1016 045"
            ),
            IbanCountryTestData(
                name = "Mali",
                swift = false,
                sepa = false,
                plain = "ML13ML0160120102600100668497",
                bank = null,
                branch = null,
                pretty = "ML13 ML01 6012 0102 6001 0066 8497"
            ),
            IbanCountryTestData(
                name = "Morocco",
                swift = false,
                sepa = false,
                plain = "MA64011519000001205000534921",
                bank = null,
                branch = null,
                pretty = "MA64 0115 1900 0001 2050 0053 4921"
            ),
            IbanCountryTestData(
                name = "Mozambique",
                swift = false,
                sepa = false,
                plain = "MZ59000301080016367102371",
                bank = null,
                branch = null,
                pretty = "MZ59 0003 0108 0016 3671 0237 1"
            ),
            IbanCountryTestData(
                name = "Niger",
                swift = false,
                sepa = false,
                plain = "NE58NE0380100100130305000268",
                bank = null,
                branch = null,
                pretty = "NE58 NE03 8010 0100 1303 0500 0268"
            ),
            IbanCountryTestData(
                name = "Senegal",
                swift = false,
                sepa = false,
                plain = "SN08SN0100152000048500003035",
                bank = null,
                branch = null,
                pretty = "SN08 SN01 0015 2000 0485 0000 3035"
            ),
            IbanCountryTestData(
                name = "Togo",
                swift = false,
                sepa = false,
                plain = "TG53TG0090604310346500400070",
                bank = null,
                branch = null,
                pretty = "TG53 TG00 9060 4310 3465 0040 0070"
            )
        )
            .sortedBy { it.name }


        // Table for parametrized tests
        val countriesTestDataTable =
            tableOf("Test data")
                .run {
                    var table: Table1<IbanCountryTestData>? = null
                    testData
                        .forEach {
                            table = table?.row(it) ?: row(it)
                        }
                    requireNotNull(table)
                }
    }
}
