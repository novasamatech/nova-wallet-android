package io.novafoundation.nova.feature_swap_impl.data.exchanger

import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById
import java.io.IOException

class ExchangerAAssetsRepository(
    private val source: ExchangerAQuoteSource,
    private val chainRegistry: ChainRegistry
) {

    suspend fun assets(): List<ExchangerAAsset> {
        val remoteAssets = source.assets()
        val chains = chainRegistry.enabledChainById()

        return remoteAssets.mapNotNull { remote ->
            val chainId = remote.chainId ?: throw IOException("Missing Exchanger A chainId")
            val assetId = remote.assetId ?: throw IOException("Missing Exchanger A assetId")
            val canSend = remote.send ?: throw IOException("Missing Exchanger A send flag")
            val canReceive = remote.receive ?: throw IOException("Missing Exchanger A receive flag")
            val chainAsset = chains[chainId]?.assetsById?.get(assetId) ?: return@mapNotNull null

            ExchangerAAsset(chainAsset, canSend, canReceive)
        }
    }
}
