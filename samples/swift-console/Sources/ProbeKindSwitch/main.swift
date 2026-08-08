import Foundation
import Kiban

// Risky half of the Malformed.Kind probe: attempts to switch over Kind by guessed Swift
// case names (Kotlin/Native's usual SCREAMING_CASE -> lowerCamelCase conversion) and to
// compare it with `==`. If Kind isn't exported as a genuine Swift enum, this whole file
// fails to compile — see ProbeKindDescribe for output that survives that.

let result = Iban.companion.parse(input: "")
guard let malformed = result as? IbanParseExceptionMalformed else {
    print("as? IbanParseExceptionMalformed failed for empty input — got \(type(of: result))")
    exit(1)
}

let kind = malformed.kind
print("kind == kind: \(kind == kind)")

switch kind {
case .empty:
    print("kind switch: .empty matched — Kind is a genuine, exhaustively-switchable Swift enum")
case .invalidBoundaryCharacter:
    print("kind switch: .invalidBoundaryCharacter")
case .tooShort:
    print("kind switch: .tooShort")
case .nonNumericCheckDigits:
    print("kind switch: .nonNumericCheckDigits")
case .invalidCharacter:
    print("kind switch: .invalidCharacter")
case .invalidStructure:
    print("kind switch: .invalidStructure")
}
