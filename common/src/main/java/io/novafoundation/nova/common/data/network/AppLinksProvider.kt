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

    val paritySignerTroubleShooting: String,
    val polkadotVaultTroubleShooting: String,
    val ledgerBluetoothGuide: String,
) {

    fun getTwitterAccountUrl(
        accountName: String
    ): String = twitterAccountTemplate.format(accountName)
}
