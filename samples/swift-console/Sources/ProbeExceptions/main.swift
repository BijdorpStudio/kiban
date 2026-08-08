import Kiban

// #9's other open question: the sealed IbanParseException hierarchy arrives as a plain
// class hierarchy through Objective-C interop, so Swift can't switch over it exhaustively.
// This probes what typed, non-exhaustive inspection (`as?` per subtype) actually looks
// like. Malformed.Kind and the undeclared-exception crash behavior are separate probes —
// see ProbeKindDescribe/ProbeKindSwitch/ProbeUndeclaredThrow.

func inspect(_ label: String, _ input: String) {
    print("--- \(label): \(input) ---")
    let result = Iban.companion.parse(input: input)
    guard let error = result.exceptionOrNull() else {
        print("unexpectedly succeeded")
        return
    }
    print("runtime type: \(type(of: error))")
    print("message: \(error.message ?? "<no message>")")

    if let malformed = error as? IbanParseExceptionMalformed {
        print("case: Malformed, kind = \(malformed.kind)")
    } else if let unknownCountry = error as? IbanParseExceptionUnknownCountryCode {
        print("case: UnknownCountryCode, countryCode = \(unknownCountry.countryCode)")
    } else if let wrongLength = error as? IbanParseExceptionWrongLength {
        print(
            "case: WrongLength, expected = \(wrongLength.expectedLength), actual = \(wrongLength.actualLength)"
        )
    } else if error is IbanParseExceptionWrongChecksum {
        print("case: WrongChecksum")
    } else {
        print(
            "case: none of the four known subtypes matched via `as?` — exhaustiveness is not possible from Swift, this branch would be the silent-failure trap in real client code"
        )
    }
}

inspect("empty", "")
inspect("too short", "NL9")
inspect("unknown country", "ZZ91ABNA0417164300")
inspect("wrong length", "NL91ABNA041716430")
inspect("wrong checksum", "NL91ABNA0417164301")
