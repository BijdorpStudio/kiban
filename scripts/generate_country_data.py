#!/usr/bin/env python3
"""Generate CountryCodesData.kt and CountryTestData.kt from the SWIFT IBAN Registry TXT.

The SWIFT registry TXT (https://www.swift.com/standards/data-standards/iban,
"IBAN Registry (TXT)") must be downloaded manually in a browser — the endpoint
blocks non-browser HTTP clients — and must NOT be committed to this repository.

Usage:
    python3 scripts/generate_country_data.py --registry ~/Downloads/iban-registry-v102.txt --rev 102

The registry file is transposed: rows are fields, countries are columns.
An overlay (scripts/overlay.json) adds IBAN.com experimental-list countries and
EPC SEPA participation the SWIFT registry lags behind on.
"""

import argparse
import csv
import datetime
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DATA_KT = REPO / "library/src/commonMain/kotlin/nl/bijdorpstudio/kiban/CountryCodesData.kt"
TEST_KT = REPO / "library/src/commonTest/kotlin/nl/bijdorpstudio/kiban/CountryTestData.kt"

LICENSE_HEADER = """/*
   Copyright {year} Barend Garvelink, Eugen Martynov

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
"""


def mod97(iban: str) -> int:
    rearranged = iban[4:] + iban[:4]
    digits = "".join(str(int(c, 36)) for c in rearranged)
    return int(digits) % 97


def parse_position(cell: str) -> tuple[int, int]:
    """Parse a '1-4' style position within the BBAN into (begin, end) String.substring
    indices within the full IBAN, or (0, 0) if absent."""
    m = re.match(r"^(\d+)-(\d+)$", cell.strip())
    if not m:
        return 0, 0
    start, end = int(m.group(1)), int(m.group(2))
    return 4 + start - 1, 4 + end


def parse_registry(path: Path) -> list[dict]:
    rows: dict[str, list[str]] = {}
    with open(path, newline="", encoding="utf-8", errors="replace") as f:
        for row in csv.reader(f, delimiter="\t"):
            if row:
                rows[row[0].strip()] = [c.strip() for c in row[1:]]

    codes = rows["IBAN prefix country code (ISO 3166)"]
    count = len([c for c in codes if c])

    def field(label: str) -> list[str]:
        values = rows.get(label, [])
        return values + [""] * (count - len(values))

    names = field("Name of country")
    sepa = field("SEPA country")
    bank_pos = field("Bank identifier position within the BBAN")
    branch_pos = field("Branch identifier position within the BBAN")
    lengths = field("IBAN length")
    examples = field("IBAN electronic format example")

    countries = []
    for i in range(count):
        cc = codes[i]
        countries.append(
            {
                "code": cc,
                "name": names[i],
                "swift": True,
                "sepa": sepa[i] == "Yes",
                "length": int(lengths[i]),
                "bank": parse_position(bank_pos[i]),
                "branch": parse_position(branch_pos[i]),
                "example": examples[i].replace(" ", ""),
            }
        )
    return countries


def apply_overlay(countries: list[dict], overlay: dict) -> list[dict]:
    by_code = {c["code"]: c for c in countries}
    for cc in overlay["sepa_overrides"]["codes"]:
        if cc in by_code:
            by_code[cc]["sepa"] = True
    for entry in overlay["experimental"]:
        if entry["code"] in by_code:
            sys.exit(
                f"Overlay country {entry['code']} is now in the SWIFT registry; "
                "remove it from scripts/overlay.json."
            )
        countries.append(
            {
                "code": entry["code"],
                "name": entry["name"],
                "swift": False,
                "sepa": False,
                "length": len(entry["example"]),
                "bank": (0, 0),
                "branch": (0, 0),
                "example": entry["example"],
            }
        )
    return sorted(countries, key=lambda c: c["code"])


def validate(countries: list[dict]) -> None:
    for c in countries:
        ex = c["example"]
        assert ex.startswith(c["code"]), f"{c['code']}: example {ex} has wrong prefix"
        assert len(ex) == c["length"], (
            f"{c['code']}: example length {len(ex)} != declared length {c['length']}"
        )
        assert mod97(ex) == 1, f"{c['code']}: example {ex} fails mod-97 check"
        for begin, end in (c["bank"], c["branch"]):
            assert 0 <= begin <= end <= c["length"], f"{c['code']}: bad identifier range"


def pretty(plain: str) -> str:
    return " ".join(plain[i : i + 4] for i in range(0, len(plain), 4))


def generate_data_kt(countries: list[dict], rev: str, date: str) -> str:
    year = date[:4]
    lengths = []
    bank_branch = []
    for c in countries:
        flags = "".join(
            [" or SWIFT" if c["swift"] else "", " or SEPA" if c["sepa"] else ""]
        )
        lengths.append(f"        /* {c['code']} */ {c['length']}{flags},")
        (bb, be), (rb, re_) = c["bank"], c["branch"]
        bank_branch.append(
            f"        /* {c['code']} */ {bb}\n"
            f"                or (({bb} + {be - bb}) shl BANK_IDENTIFIER_END_SHIFT)\n"
            f"                or ({rb} shl BRANCH_IDENTIFIER_BEGIN_SHIFT)\n"
            f"                or (({rb} + {re_ - rb}) shl BRANCH_IDENTIFIER_END_SHIFT),"
        )
    codes = "\n".join(f'        "{c["code"]}",' for c in countries)
    return f"""{LICENSE_HEADER.format(year=year)}
/**
 * Contains information about IBAN country codes. This is a generated file, do not edit manually.
 * Regenerate with: python3 scripts/generate_country_data.py (see scripts/generate_country_data.py --help).
 * Updated to SWIFT IBAN Registry version {rev} on {date}.
 */
internal object CountryCodesData {{
    /**
     * The "yyyy-MM-dd" datestamp that the embedded IBAN data was updated.
     */
    const val LAST_UPDATE_DATE = "{date}"

    /**
     * The revision of the SWIFT IBAN Registry to which the embedded IBAN data was updated.
     */
    const val LAST_UPDATE_REV = "{rev}"

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
{codes}
    )

    /**
     * Lengths for each country's IBAN. The indices match the indices of [.COUNTRY_CODES], the values are the
     * expected length. Values may embed the [.SEPA] and [.SWIFT] flags to indicate the SEPA membership and
     * whether the record is listed in the SWIFT IBAN Registry.
     */
    val COUNTRY_IBAN_LENGTHS: IntArray = intArrayOf(
{chr(10).join(lengths)}
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
{chr(10).join(bank_branch)}
    )
}}
"""


def generate_test_kt(countries: list[dict], rev: str, date: str) -> str:
    year = date[:4]
    entries = []
    for c in sorted(countries, key=lambda c: (not c["swift"], c["name"])):
        plain = c["example"]
        (bb, be), (rb, re_) = c["bank"], c["branch"]
        bank = f'"{plain[bb:be]}"' if bb else "null"
        branch = f'"{plain[rb:re_]}"' if rb else "null"
        name = c["name"].replace('"', '\\"')
        entries.append(
            f"    IbanCountryTestData(\n"
            f'        name = "{name}",\n'
            f"        swift = {str(c['swift']).lower()},\n"
            f"        sepa = {str(c['sepa']).lower()},\n"
            f'        plain = "{plain}",\n'
            f"        bank = {bank},\n"
            f"        branch = {branch},\n"
            f'        pretty = "{pretty(plain)}"\n'
            f"    ),"
        )
    return f"""{LICENSE_HEADER.format(year=year)}
/**
 * Valid example IBANs for every known country. This is a generated file, do not edit manually.
 * Regenerate with: python3 scripts/generate_country_data.py (see scripts/generate_country_data.py --help).
 * Updated to SWIFT IBAN Registry version {rev} on {date}.
 *
 * References:
 * - SWIFT IBAN Registry: https://www.swift.com/standards/data-standards/iban
 * - IBAN.com Experimental List: https://www.iban.com/structure
 */
internal val countryTestData = listOf(
{chr(10).join(entries)}
)
"""


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", required=True, help="Path to the SWIFT registry TXT file")
    parser.add_argument("--rev", required=True, help="SWIFT registry revision, e.g. 102")
    parser.add_argument(
        "--date", default=datetime.date.today().isoformat(), help="Update date (yyyy-MM-dd)"
    )
    args = parser.parse_args()

    overlay = json.loads((REPO / "scripts/overlay.json").read_text())
    countries = apply_overlay(parse_registry(Path(args.registry)), overlay)
    validate(countries)

    DATA_KT.write_text(generate_data_kt(countries, args.rev, args.date))
    TEST_KT.write_text(generate_test_kt(countries, args.rev, args.date))
    swift_count = sum(1 for c in countries if c["swift"])
    print(f"Generated data for {len(countries)} countries ({swift_count} SWIFT, "
          f"{len(countries) - swift_count} experimental) at registry rev {args.rev}.")
    print(f"Wrote {DATA_KT.relative_to(REPO)} and {TEST_KT.relative_to(REPO)}.")


if __name__ == "__main__":
    main()
