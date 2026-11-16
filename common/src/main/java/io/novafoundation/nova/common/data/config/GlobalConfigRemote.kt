package io.novafoundation.nova.common.data.config

class GlobalConfigRemote(
    val multisigsApiUrl: String,
    val proxyApiUrl: String,
    val multiStakingApiUrl: String,
    val infraUrl: String?,
    val defaultSwapPair: SwapPairRemote? = null
)

class SwapPairRemote(val assetIn: Asset?, val assetOut: Asset?) {
    class Asset(val chainId: String?, val assetId: Int?)
}
