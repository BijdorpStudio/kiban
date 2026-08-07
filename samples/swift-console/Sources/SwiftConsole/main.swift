import Kiban

let iban = "NL91ABNA0417164300".toIbanOrNull()!
print("parse: \(iban.plain)")

print("isValidIban(not an iban): \("not an iban".isValidIban())")

let orNull = "NL91ABNA0417164301".toIbanOrNull()
print("toIbanOrNull (wrong check digits): \(String(describing: orNull))")

print("isValidIban: \(iban.plain.isValidIban())")

// Result<Iban> and the sealed IbanParseException hierarchy are left out here: how those surface
// from Swift is #9's own open question, not something to guess at in this sample.

print("formatted: \(iban.description)")
print("plain: \(iban.plain)")

let anotherIban = "BE68 5390 0754 7034".toIbanOrNull()!
print("parsed formatted input: \(anotherIban.plain)")

let ibanAgain = "NL91ABNA0417164300".toIbanOrNull()!
print("equals: \(iban.isEqual(ibanAgain))")

let candidate = "GB29 NWBK 6016 1331 9268 19"
print("verifyCheckDigits: \(Modulo97.shared.verifyCheckDigits(input: candidate))")

let composed = Iban.companion.compose(countryCode: "BI", bban: "10000100010000332045181")
print("compose: \(composed)")

let sepaIban = candidate.toIbanOrNull()!
print("isSEPA: \(sepaIban.isSEPA)")
print("isInSwiftRegistry: \(sepaIban.isInSwiftRegistry)")

print(
    "calculateCheckDigits(GB, ...): \(Modulo97.shared.calculateCheckDigits(countryCode: "GB", bban: "NWBK60161331926819"))"
)
print("calculateCheckDigits(XX, X): \(Modulo97.shared.calculateCheckDigits(countryCode: "XX", bban: "X"))")

print("getLength(DK): \(CountryCodes.shared.getLength(countryCode: "DK"))")

print("bankIdentifier: \(String(describing: iban.bankIdentifier))")
print("branchIdentifier: \(String(describing: iban.branchIdentifier))")
