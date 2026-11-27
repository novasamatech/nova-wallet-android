package io.novafoundation.nova.feature_swap_impl.domain.exchanger.catalogue

import io.novafoundation.nova.feature_swap_impl.data.exchanger.ExchangerAAssetsRepository
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/** Release 1 supports all backend-enabled Polkadot Asset Hub assets into native TAO. */
class RealExchangerATaoCatalogue(
    private val assetsRepository: ExchangerAAssetsRepository
) : ExchangerATaoCatalogue {

    override suspend fun availablePairs(): List<ExchangerASwapPair> {
        val assets = assetsRepository.assets()
        val receiveAsset = assets.firstOrNull { it.chainAsset.fullId == ExchangerATaoCatalogue.receiveAsset && it.canReceive } ?: return emptyList()

        return assets
            .filter { it.chainAsset.chainId == Chain.Geneses.POLKADOT_ASSET_HUB && it.canSend }
            .map { ExchangerASwapPair(it.chainAsset, receiveAsset.chainAsset) }
    }
}
