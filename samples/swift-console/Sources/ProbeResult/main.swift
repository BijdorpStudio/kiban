import Foundation
import Kiban

// #9's central open question: Result<Iban> is an inline value class in Kotlin. A prior probe
// run confirmed the sharpest possible version of the concern: Iban.parse's return type is
// erased to plain `Any?` in Swift — no `Result`-shaped wrapper survives the interop boundary
// at all, so `.isSuccess`/`.getOrNull()`/`.exceptionOrNull()` do not exist. This probes what,
// if anything, is still recoverable from that bare `Any?` via runtime `as?` casts to real,
// unambiguous Kotlin types (Iban itself, and IbanParseException) rather than guessing at any
// interop-generated wrapper type name.

func probe(_ label: String, _ input: String) {
    let result = Iban.companion.parse(input: input)
    print("--- \(label): \(input) ---")
    print("dynamic type: \(type(of: result))")
    print("description: \(String(describing: result))")

    if let iban = result as? Iban {
        print("as? Iban succeeded: \(iban.plain)")
        return
    }
    print("as? Iban failed")

    if let error = result as? IbanParseException {
        print("as? IbanParseException succeeded: \(error.message ?? "<no message>")")
    } else {
        print("as? IbanParseException ALSO failed — the failure is not recoverable this way")
    }
}

probe("success", "NL91ABNA0417164300")
probe("wrong checksum", "NL91ABNA0417164301")
probe("unknown country", "ZZ91ABNA0417164300")
probe("too short", "NL9")
probe("empty", "")

// Iban.compose also returns Result<Iban>; exercise its failure path too.
print("--- compose failure ---")
let composed = Iban.companion.compose(countryCode: "ZZ", bban: "0")
print("dynamic type: \(type(of: composed))")
print("description: \(String(describing: composed))")
if let error = composed as? IbanParseException {
    print("as? IbanParseException succeeded: \(error.message ?? "<no message>")")
}
