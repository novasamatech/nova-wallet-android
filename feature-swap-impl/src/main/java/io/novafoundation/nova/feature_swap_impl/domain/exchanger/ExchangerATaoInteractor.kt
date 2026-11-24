package io.novafoundation.nova.feature_swap_impl.domain.exchanger

import io.novafoundation.nova.feature_swap_impl.data.exchanger.ExchangerAAssetsRepository
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAssetOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/** Release 1 supports all backend-enabled Polkadot Asset Hub assets into native TAO. */
class ExchangerATaoInteractor(
    private val assetsRepository: ExchangerAAssetsRepository
) {

    private val receiveAssetId = FullChainAssetId.utilityAssetOf(Chain.Geneses.BITTENSOR)

    suspend fun availablePairs(): List<ExchangerASwapPair> {
        val assets = assetsRepository.assets()
        val receiveAsset = assets.firstOrNull { it.chainAsset.fullId == receiveAssetId && it.canReceive } ?: return emptyList()

        return assets
            .filter { it.chainAsset.chainId == Chain.Geneses.POLKADOT_ASSET_HUB && it.canSend }
            .map { ExchangerASwapPair(it.chainAsset, receiveAsset.chainAsset) }
    }
}
