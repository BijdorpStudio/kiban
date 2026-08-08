import Foundation
import Kiban

// Risky half of the Malformed.Kind probe: attempts to switch over Kind by its confirmed
// Swift case-property names (.empty, .tooShort, etc — these matched the real header) and to
// compare it with `==`. The real header shows Kind (IbanParseException.MalformedKind) as an
// Objective-C class hierarchy under KibanKotlinEnum<E>, not an NS_ENUM — the same "Kotlin
// enum, not a Swift enum" limitation the issue itself names for the plain ObjC path. If
// Swift's importer doesn't bridge that as a genuine, switchable enum, this whole file fails
// to compile — see ProbeKindDescribe for output that survives that either way (the cast
// itself is expected to fail at runtime regardless; see that file's comment).

let result = Iban.companion.parse(input: "")
guard let malformed = result as? IbanParseException.Malformed else {
    print("as? IbanParseException.Malformed failed for empty input — got \(type(of: result))")
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
