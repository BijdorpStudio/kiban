package nl.bijdorpstudio.kiban

/**
 * Alias for [Iban] matching the class name used by the original `java-iban` library, for callers
 * migrating from it. New code should write [Iban].
 *
 * JVM-only, and kept for 1.0; see docs/209-api-design-notes.md.
 */
public typealias IBAN = Iban
