package io.novafoundation.nova.feature_swap_impl.domain.exchanger.catalogue

import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.utilityAssetOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface ExchangerATaoCatalogue {

    companion object {
        val receiveAsset = FullChainAssetId.utilityAssetOf(Chain.Geneses.BITTENSOR)
    }

    suspend fun availablePairs(): List<ExchangerASwapPair>
}

/** Candidate pair from the catalogue. A quote confirms current pair availability. */
class ExchangerASwapPair(
    val assetIn: Chain.Asset,
    val assetOut: Chain.Asset
)
