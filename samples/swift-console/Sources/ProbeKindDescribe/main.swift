import Foundation
import Kiban

// Ground truth from the real generated header (dumped by ios-interop-verify.yml): Kotlin's
// nested IbanParseException.Malformed keeps genuine Swift nested-type syntax
// (`swift_name("IbanParseException.Malformed")`), not a flat concatenated name — that was
// this probe's original, wrong guess. Even with the right name, this cast is expected to
// fail at runtime: ProbeResult/ProbeExceptions already showed the erased Any? holds Kotlin's
// own internal `Result.Failure` box on the failure path, not the exception directly, so
// there is no type this can ever successfully cast to. That failure, cleanly reached instead
// of a compile error, IS the finding.

let result = Iban.companion.parse(input: "")
guard let malformed = result as? IbanParseException.Malformed else {
    print("as? IbanParseException.Malformed failed for empty input — got \(type(of: result)): \(String(describing: result))")
    exit(1)
}

let kind = malformed.kind
print("kind: \(kind)")
print("kind runtime type: \(type(of: kind))")
