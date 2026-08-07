import Kiban

// Smallest possible Swift consumer of the Kiban Kotlin/Native framework: the concrete testbed
// #9 needs to evaluate Objective-C interop (and, later, Swift Export) for real rather than by
// speculation. Sticks to the parts of the API with well-understood Kotlin/Native ObjC-export
// behavior (extension functions on String, nullable bridging, plain property getters) so this
// compiles predictably; inspecting how `Result<Iban>` and the sealed `IbanParseException`
// hierarchy actually surface from Swift is #9's own job, using this as the scaffold.
let examples = ["NL91ABNA0417164300", "NL91ABNA0417164301", "not an iban"]

for input in examples {
    if input.isValidIban(), let iban = input.toIbanOrNull() {
        print("\(input) -> valid, country \(iban.countryCode), plain \(iban.plain)")
    } else {
        print("\(input) -> invalid")
    }
}
