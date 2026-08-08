import Kiban

// Top-level Kotlin extension functions (toIbanOrNull, isValidIban — declared alongside Iban
// in the same file) are NOT exported as true Swift extensions on String. Kotlin/Native's
// default ObjC exporter puts them as static methods on an "<File>Kt" facade class
// (confirmed from the real generated header: `KibanIbanKt` / swift_name "IbanKt", with the
// receiver as the first parameter) — so `"x".toIbanOrNull()` doesn't compile; the correct
// call is `IbanKt.toIbanOrNull("x")`. This was a real, previously-unverified bug in this
// sample: ios-interop-verify.yml had never actually been run before #9 picked this up, so
// this file had never been compiled for real.

let iban = IbanKt.toIbanOrNull("NL91ABNA0417164300")!
print("parse: \(iban.plain)")

print("isValidIban(not an iban): \(IbanKt.isValidIban("not an iban"))")

let orNull = IbanKt.toIbanOrNull("NL91ABNA0417164301")
print("toIbanOrNull (wrong check digits): \(String(describing: orNull))")

print("isValidIban: \(IbanKt.isValidIban(iban.plain))")

// Result<Iban> and the sealed IbanParseException hierarchy are left out here: how those surface
// from Swift is #9's own open question — see ProbeResult/ProbeExceptions/ProbeKind*.

print("formatted: \(iban.description)")
print("plain: \(iban.plain)")

let anotherIban = IbanKt.toIbanOrNull("BE68 5390 0754 7034")!
print("parsed formatted input: \(anotherIban.plain)")

let ibanAgain = IbanKt.toIbanOrNull("NL91ABNA0417164300")!
print("equals: \(iban.isEqual(ibanAgain))")

let candidate = "GB29 NWBK 6016 1331 9268 19"
print("verifyCheckDigits: \(Modulo97.shared.verifyCheckDigits(input: candidate))")

let composed = Iban.companion.compose(countryCode: "BI", bban: "10000100010000332045181")
print("compose: \(composed)")

let sepaIban = IbanKt.toIbanOrNull(candidate)!
print("isSEPA: \(sepaIban.isSEPA)")
print("isInSwiftRegistry: \(sepaIban.isInSwiftRegistry)")

print(
    "calculateCheckDigits(GB, ...): \(Modulo97.shared.calculateCheckDigits(countryCode: "GB", bban: "NWBK60161331926819"))"
)
print("calculateCheckDigits(XX, X): \(Modulo97.shared.calculateCheckDigits(countryCode: "XX", bban: "X"))")

print("getLength(DK): \(CountryCodes.shared.getLength(countryCode: "DK"))")

print("bankIdentifier: \(String(describing: iban.bankIdentifier))")
print("branchIdentifier: \(String(describing: iban.branchIdentifier))")
