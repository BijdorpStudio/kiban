package nl.bijdorpstudio.kiban.samples.jvmcli

import nl.bijdorpstudio.kiban.Iban

/**
 * Smallest possible consumer of the published `nl.bijdorpstudio.kiban:kiban` artifact: parses an
 * IBAN passed on the command line and reports what it found.
 */
fun main(args: Array<String>) {
    val input = args.firstOrNull() ?: "NL91ABNA0417164300"
    Iban.parse(input)
        .fold(
            onSuccess = { iban ->
                println("Valid IBAN: $iban")
                println("Country: ${iban.countryCode}, SEPA: ${iban.isSEPA}")
            },
            onFailure = { failure -> println("Invalid IBAN '$input': ${failure.message}") },
        )
}
