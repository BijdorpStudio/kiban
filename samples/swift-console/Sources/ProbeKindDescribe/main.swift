import Foundation
import Kiban

// Isolated guess: Kotlin/Native's documented nested-class convention concatenates outer and
// inner class names (IbanParseException.Malformed -> IbanParseExceptionMalformed). If that
// guess is wrong, only this target (and ProbeKindSwitch, which depends on it) fails to
// compile — ProbeExceptions above already gives real signal on the base type regardless.

let result = Iban.companion.parse(input: "")
guard let malformed = result as? IbanParseExceptionMalformed else {
    print("as? IbanParseExceptionMalformed failed for empty input — got \(type(of: result))")
    exit(1)
}

let kind = malformed.kind
print("kind: \(kind)")
print("kind runtime type: \(type(of: kind))")
