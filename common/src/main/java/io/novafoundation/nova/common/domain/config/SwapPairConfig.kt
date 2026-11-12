package io.novafoundation.nova.common.domain.config

data class SwapPairConfig(val assetIn: Asset, val assetOut: Asset) {

    data class Asset(val chainId: String, val assetId: Int)
}
