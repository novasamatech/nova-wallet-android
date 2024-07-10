package io.novafoundation.nova.feature_settings_impl.domain.model

data class CustomNetworkPayload(
    val nodeUrl: String,
    val nodeName: String,
    val chainName: String,
    val tokenSymbol: String,
    val evmChainId: Int?,
    val blockExplorer: BlockExplorer?,
    val coingeckoLinkUrl: String?,
    val ignoreChainModifying: Boolean,
) {

    class BlockExplorer(
        val name: String,
        val url: String
    )
}
