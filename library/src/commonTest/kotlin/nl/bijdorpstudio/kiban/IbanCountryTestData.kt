package nl.bijdorpstudio.kiban

/**
 * Test data class for country code tests
 */
data class IbanCountryTestData(
    val name: String,
    val swift: Boolean,
    val sepa: Boolean,
    val plain: String,
    val bank: String?,
    val branch: String?,
    val pretty: String
) {
    override fun toString(): String = name
}