import SwiftUI

// If Swift Export's embed step made a `Kiban` module available to this target, `import Kiban`
// below succeeds and the API can be probed the same way samples/swift-console probes it via
// Objective-C interop, for a direct comparison. If Swift Export isn't wired up far enough for
// that yet, this import itself failing to compile is the finding — see
// ios-interop-verify.yml's "Swift Export" step for how far this got.
import Kiban

@main
struct SwiftExportProbeApp: App {
    init() {
        let result = Iban.companion.parse(input: "NL91ABNA0417164300")
        print("swift-export-probe dynamic type: \(type(of: result))")
        print("swift-export-probe description: \(String(describing: result))")
        if let iban = result as? Iban {
            print("swift-export-probe as? Iban succeeded: \(iban.plain)")
        }

        let failure = Iban.companion.parse(input: "")
        print("swift-export-probe failure description: \(String(describing: failure))")
        if let error = failure as? IbanParseException {
            print("swift-export-probe as? IbanParseException succeeded: \(error.message ?? "<nil>")")
        } else {
            print("swift-export-probe as? IbanParseException failed")
        }
    }

    var body: some Scene {
        WindowGroup {
            Text("swift export probe")
        }
    }
}
