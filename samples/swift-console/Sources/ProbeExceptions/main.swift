import Foundation
import Kiban

// #9's other open question: does the sealed IbanParseException hierarchy allow typed
// inspection from Swift? This casts the erased `Any?` failure (see ProbeResult) to the
// top-level IbanParseException type — an ordinary, non-nested, non-generic public class, so
// its exported Swift name should just be its Kotlin name. Per-subtype casts (Malformed,
// UnknownCountryCode, etc.) are their own, separately isolated probes (ProbeMalformedCast,
// ProbeKindDescribe, ProbeKindSwitch) since nested-class name generation is a real guess.

func inspect(_ label: String, _ input: String) {
    print("--- \(label): \(input) ---")
    let result = Iban.companion.parse(input: input)
    guard let error = result as? IbanParseException else {
        print("as? IbanParseException failed — got \(type(of: result)): \(String(describing: result))")
        return
    }
    print("runtime type: \(type(of: error))")
    print("message: \(error.message ?? "<no message>")")
}

inspect("empty", "")
inspect("too short", "NL9")
inspect("unknown country", "ZZ91ABNA0417164300")
inspect("wrong length", "NL91ABNA041716430")
inspect("wrong checksum", "NL91ABNA0417164301")
