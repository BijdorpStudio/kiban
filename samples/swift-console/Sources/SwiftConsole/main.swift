import Foundation
import Kiban

// Top-level Kotlin extension functions (toIbanOrNull, isValidIban, toIban) are exported as
// static methods on an `IbanKt` facade class, not as Swift extensions on String — so the call is
// `IbanKt.toIbanOrNull("x")`, not `"x".toIbanOrNull()`. See docs/9-swift-interop-review.md.

guard let iban = IbanKt.toIbanOrNull("NL91ABNA0417164300") else {
    fatalError("expected a valid IBAN")
}
print("parse: \(iban.plain)")

print("isValidIban(not an iban): \(IbanKt.isValidIban("not an iban"))")

let orNull = IbanKt.toIbanOrNull("NL91ABNA0417164301")
print("toIbanOrNull (wrong check digits): \(String(describing: orNull))")

print("isValidIban: \(IbanKt.isValidIban(iban.plain))")

// 0.5.0 made parsing strict again: Iban(input), Iban.compose(...) and toIban() throw
// IbanParseException instead of returning Result<Iban> (#9's headline finding about the 0.4.0
// API), and are annotated @Throws(IbanParseException::class) so this Objective-C interop path
// surfaces the failure as a catchable Swift error instead of aborting the process.
do {
    let unexpected = try IbanKt.toIban("not an iban")
    print("unexpected success: \(unexpected.plain)")
} catch {
    print("toIban(not an iban) failed as expected: \(error)")
}

// Kotlin's `Iban(input)` is `operator fun invoke` on the companion, which Swift reads as
// `Iban.companion.invoke(input:)` — so 0.6.0 adds the named alias `Iban.parse(input)` for callers
// outside Kotlin (#139).
do {
    let named = try Iban.companion.parse(input: "NL91 ABNA 0417 1643 00")
    print("Iban.companion.parse: \(named.plain)")
} catch {
    fatalError("expected a valid IBAN, got \(error)")
}

// The open question left by docs/9-swift-interop-review.md: a caught IbanParseException is
// reachable as a description string, but does userInfo["KotlinException"] downcast to the real
// typed exception? This prints the answer either way rather than asserting one, so the run reports
// what the toolchain actually does.
do {
    let unexpected = try Iban.companion.parse(input: "NL91ABNA0417164301")
    print("unexpected success: \(unexpected.plain)")
} catch {
    let kotlinException = (error as NSError).userInfo["KotlinException"]
    print("KotlinException userInfo entry: \(String(describing: kotlinException))")
    if let parseFailure = kotlinException as? IbanParseException {
        print("downcast to IbanParseException: succeeded")
        switch parseFailure {
        case let malformed as IbanParseException.Malformed:
            print("typed access: Malformed, kind \(malformed.kind)")
        case let wrongLength as IbanParseException.WrongLength:
            print("typed access: WrongLength, expected \(wrongLength.expectedLength), actual \(wrongLength.actualLength)")
        case let unknown as IbanParseException.UnknownCountryCode:
            print("typed access: UnknownCountryCode, \(unknown.countryCode)")
        case is IbanParseException.WrongChecksum:
            print("typed access: WrongChecksum")
        default:
            print("typed access: unrecognized IbanParseException subtype")
        }
    } else {
        print("downcast to IbanParseException: failed — only the description is reachable")
    }
}

print("formatted: \(iban.description)")
print("plain: \(iban.plain)")

guard let anotherIban = IbanKt.toIbanOrNull("BE68 5390 0754 7034") else {
    fatalError("expected a valid IBAN")
}
print("parsed formatted input: \(anotherIban.plain)")

let ibanAgain = IbanKt.toIbanOrNull("NL91ABNA0417164300")
print("equals: \(iban == ibanAgain)")

let candidate = "GB29 NWBK 6016 1331 9268 19"
print("verifyCheckDigits: \(Modulo97.shared.verifyCheckDigits(input: candidate))")

guard let sepaIban = IbanKt.toIbanOrNull(candidate) else {
    fatalError("expected a valid IBAN")
}
print("isSEPA: \(sepaIban.isSEPA)")
print("isInSwiftRegistry: \(sepaIban.isInSwiftRegistry)")

print(
    "calculateCheckDigits(GB, ...): \(Modulo97.shared.calculateCheckDigits(countryCode: "GB", bban: "NWBK60161331926819"))"
)
print("calculateCheckDigits(XX, X): \(Modulo97.shared.calculateCheckDigits(countryCode: "XX", bban: "X"))")

print("getLength(DK): \(String(describing: CountryCodes.shared.getLength(countryCode: "DK")))")

print("bankIdentifier: \(String(describing: iban.bankIdentifier))")
print("branchIdentifier: \(String(describing: iban.branchIdentifier))")
