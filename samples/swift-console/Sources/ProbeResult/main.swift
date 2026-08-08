import Kiban

// #9's central open question: Result<Iban> is an inline value class in Kotlin, so from
// Swift there is no compiler-checked typed access to the success value or the failure —
// this probes exactly what *is* available, for real, through Objective-C interop.

func probe(_ label: String, _ input: String) {
    let result = Iban.companion.parse(input: input)
    print("--- \(label): \(input) ---")
    print("static type: \(type(of: result))")
    print("isSuccess: \(result.isSuccess)")
    print("isFailure: \(result.isFailure)")

    if let value = result.getOrNull() {
        print("getOrNull(): \(type(of: value)) -> \(value)")
        if let iban = value as? Iban {
            print("as? Iban succeeded: \(iban.plain)")
        } else {
            print("as? Iban FAILED — getOrNull()'s static type does not carry Iban")
        }
    } else {
        print("getOrNull(): nil")
    }

    if let error = result.exceptionOrNull() {
        print("exceptionOrNull(): \(type(of: error)) -> \(error)")
        if let parseError = error as? IbanParseException {
            print("as? IbanParseException succeeded: \(parseError.message ?? "<no message>")")
        } else {
            print("as? IbanParseException FAILED")
        }
    } else {
        print("exceptionOrNull(): nil")
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
print("static type: \(type(of: composed))")
print("isFailure: \(composed.isFailure)")
if let error = composed.exceptionOrNull() {
    print("exceptionOrNull(): \(type(of: error)) -> \(error)")
}
