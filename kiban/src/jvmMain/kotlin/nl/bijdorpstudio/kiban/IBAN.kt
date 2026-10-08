package nl.bijdorpstudio.kiban

/**
 * Alias for [Iban] matching the class name used by the original `java-iban` library, for callers
 * migrating from it.
 *
 * The alias is JVM-only. It is declared in `jvmMain`, so a `commonMain` consumer — and every
 * non-JVM target — sees only [Iban]. That is deliberate rather than an oversight: `java-iban` is a
 * JVM library, so a caller migrating from it is by definition compiling for the JVM, and the only
 * way to make the name visible in common code would be an `expect`/`actual` pair that puts a
 * spelling convenience into the multiplatform API surface no multiplatform caller asked for.
 *
 * It is kept for 1.0. A typealias is erased at compile time — no class file is generated, nothing
 * appears in the JVM API dump, and there is no second type for `Iban` to stay compatible with — so
 * carrying it costs nothing, while removing it would break the source compatibility of exactly the
 * migrating callers it exists for. New code should write [Iban]; `IBAN` is here so that a
 * `java-iban` port compiles with its import changed and nothing else.
 */
public typealias IBAN = Iban
