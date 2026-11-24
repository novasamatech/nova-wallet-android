package io.novafoundation.nova.feature_swap_impl.domain.exchanger

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ExchangerAAsset(
    val chainAsset: Chain.Asset,
    val canSend: Boolean,
    val canReceive: Boolean
)

/** Candidate pair from the catalogue. A quote confirms current pair availability. */
class ExchangerASwapPair(
    val assetIn: Chain.Asset,
    val assetOut: Chain.Asset
)
