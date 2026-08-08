import Kiban

// Safe half of the Malformed.Kind probe: describe the value without guessing at Swift
// case names, so it survives even if ProbeKindSwitch's guess is wrong. See that target
// for whether Kind arrives as a genuine, exhaustively-switchable Swift enum.

let result = Iban.companion.parse(input: "")
guard let malformed = result.exceptionOrNull() as? IbanParseExceptionMalformed else {
    print("expected a Malformed failure for empty input, got something else")
    exit(1)
}

let kind = malformed.kind
print("kind: \(kind)")
print("kind runtime type: \(type(of: kind))")
