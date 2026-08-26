# Script test data

These are **invented** registry files, not SWIFT data. The real IBAN Registry TXT is not
redistributable and is never committed (see the repository README); these fixtures only imitate its
format so that `kotlin scripts/generate_country_data.main.kts --self-check` can exercise the parser
offline, on every run rather than once a week when the registry actually changes.

Their countries use ISO 3166 user-assigned codes (`XA`–`XH`), which no registry revision can ever
hand out, so a fixture entry can never be mistaken for — or collide with — a real one.

## `synthetic-registry.txt`

The format quirks the parser has to survive, one per country:

| Country | What it covers                                                                        |
|---------|---------------------------------------------------------------------------------------|
| `XE`    | Bank and branch identifier ranges; a branch identifier example stated as `N/A`          |
| `XA`    | The ordinary case: both identifiers, both examples, SEPA member                         |
| `XD`    | A country embedding neither identifier (`N/A` positions)                                |
| `XC`    | Whitespace: padded cells, an example IBAN in print format, identifiers written spaced   |
| `XB`    | A bank identifier without a branch identifier                                           |
| `XF`    | The last column of a row that stops short, as a truncated download would                |

The columns are deliberately out of alphabetical order, because the generator sorts them; one cell
is quoted across two lines, as the real registry's longer fields are.

## `synthetic-registry-minimal.txt`

A registry stripped to the rows the parser cannot do without. A revision that renames a column takes
that field away from every country at once, and what it carried then has to read as absent rather
than throw — which a row merely stopping short does not test, because the TSV reader pads those
back out on its own.

## Both

Every example IBAN passes the mod-97 check and matches its declared length, so both fixtures survive
the generator's own validation — the point is to exercise the parser, not to test that validation
rejects them. The self-check corrupts copies of these entries in memory for that.
