package io.novafoundation.nova.feature_swap_api.domain.exchanger.catalogue

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface ExchangerATaoCatalogue {

    suspend fun supportsPair(pay: FullChainAssetId, receive: FullChainAssetId): Boolean

    suspend fun availablePairs(): List<ExchangerASwapPair>
}

/** Candidate pair from the catalogue. A quote confirms current pair availability. */
class ExchangerASwapPair(
    val assetIn: Chain.Asset,
    val assetOut: Chain.Asset
)
