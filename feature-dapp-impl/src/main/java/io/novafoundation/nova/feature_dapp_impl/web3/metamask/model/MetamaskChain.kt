package io.novafoundation.nova.feature_dapp_impl.web3.metamask.model

data class MetamaskChain(
    val chainId: String,
    val chainName: String,
    val nativeCurrency: NativeCurrency,
    val rpcUrls: List<String>,
    val blockExplorerUrls: List<String>?,
    val iconUrls: List<String>?
) {

    companion object {

        val ETHEREUM = MetamaskChain(
            chainId = "0x1",
            chainName = "Ethereum Mainnet",
            nativeCurrency = NativeCurrency(name = "Ether", symbol = "ETH", decimals = 18),
            rpcUrls = listOf("https://mainnet.infura.io/v3/6b7733290b9a4156bf62a4ba105b76ec"),
            blockExplorerUrls = null,
            iconUrls = null
        )
    }

    class NativeCurrency(
        val name: String,
        val symbol: String,
        val decimals: Int
    )
}
