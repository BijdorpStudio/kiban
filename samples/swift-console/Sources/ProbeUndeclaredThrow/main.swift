import Kiban

// Neither Iban.valueOf nor the Result.getOrThrow() it calls are annotated @Throws, which
// is what makes a Kotlin exception a catchable Swift error at the interop boundary.
// Kotlin/Native's documented default for an *undeclared* exception crossing into
// Swift/Objective-C is to terminate the process, not hand back something catchable.
// This probe calls it without `try` (a `try` here would itself fail to compile if Swift
// doesn't see this as throwing) and expects the process to crash on the following line —
// confirming that in isolation, in its own target, so it can't take any other probe's
// output down with it.
print("--- undeclared throw probe: Iban.valueOf on invalid input ---")
let iban = Iban.companion.valueOf(input: "not an iban")
print("did NOT crash — unexpectedly succeeded: \(iban.plain)")
