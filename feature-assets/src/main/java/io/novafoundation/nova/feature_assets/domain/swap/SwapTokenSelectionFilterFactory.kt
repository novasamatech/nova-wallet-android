package io.novafoundation.nova.feature_assets.domain.swap

import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class SwapTokenSelectionFilterFactory {

    fun createChainFilter(enabledChains: List<Chain>): Filter<Chain> {
        val relayChainIds = enabledChains.mapNotNull { it.parentId }.toSet() + setOf(ChainGeneses.POLKADOT, ChainGeneses.KUSAMA)

        return SwapSelectionChainFilter(relayChainIds)
    }

    fun createAssetNetworkFilter(selectedNetworkIds: Set<ChainId>): Filter<Asset> {
        return SelectedNetworksAssetFilter(selectedNetworkIds)
    }

    fun createOppositeAssetFilter(oppositeAssetId: FullChainAssetId?): Filter<Asset> {
        return ExcludeOppositeAssetFilter(oppositeAssetId)
    }
}

private class SwapSelectionChainFilter(
    private val relayChainIds: Set<ChainId>
) : Filter<Chain> {

    override fun shouldInclude(model: Chain): Boolean {
        // nova-utils currently marks paused networks in their name; connectionState is the user's sync preference.
        val isPaused = model.name.endsWith("(PAUSED)", ignoreCase = true)
        val isRelayChain = model.id in relayChainIds

        return !isPaused && !isRelayChain
    }
}

private class SelectedNetworksAssetFilter(
    private val selectedNetworkIds: Set<ChainId>
) : Filter<Asset> {

    override fun shouldInclude(model: Asset): Boolean {
        return selectedNetworkIds.isEmpty() || model.token.configuration.chainId in selectedNetworkIds
    }
}

private class ExcludeOppositeAssetFilter(
    private val oppositeAssetId: FullChainAssetId?
) : Filter<Asset> {

    override fun shouldInclude(model: Asset): Boolean {
        return model.token.configuration.fullId != oppositeAssetId
    }
}
