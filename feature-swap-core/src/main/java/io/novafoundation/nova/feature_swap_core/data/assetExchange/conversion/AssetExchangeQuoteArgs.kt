package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion

import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

data class AssetExchangeQuoteArgs(
    val chainAssetIn: Chain.Asset,
    val chainAssetOut: Chain.Asset,
    val amount: BigInteger,
    val swapDirection: SwapDirection,
)
