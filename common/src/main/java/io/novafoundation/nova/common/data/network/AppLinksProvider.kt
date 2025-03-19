package io.novafoundation.nova.common.data.network

class AppLinksProvider(
    val termsUrl: String,
    val privacyUrl: String,
    val telegram: String,
    val twitter: String,
    val rateApp: String,
    val website: String,
    val github: String,
    val email: String,
    val youtube: String,

    val payoutsLearnMore: String,
    val recommendedValidatorsLearnMore: String,
    val twitterAccountTemplate: String,
    val setControllerLearnMore: String,
    val setControllerDeprecatedLeanMore: String,

    val paritySignerTroubleShooting: String,
    val polkadotVaultTroubleShooting: String,
    val ledgerBluetoothGuide: String,
    val wikiBase: String,
    val wikiProxy: String,
    val integrateNetwork: String,
    val storeUrl: String,

    val ledgerMigrationArticle: String,
    val unifiedAddressArticle: String
) {

    fun getTwitterAccountUrl(
        accountName: String
    ): String = twitterAccountTemplate.format(accountName)
}
