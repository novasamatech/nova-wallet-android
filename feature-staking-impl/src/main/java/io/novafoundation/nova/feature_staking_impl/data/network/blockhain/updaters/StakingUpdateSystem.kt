package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.updaters.SingleChainUpdateSystem
import io.novafoundation.nova.runtime.state.SingleAssetSharedState

class StakingUpdateSystem(
    private val relaychainUpdaters: List<Updater>,
    private val parachainUpdaters: List<Updater>,
    private val commonUpdaters: List<Updater>,
    chainRegistry: ChainRegistry,
    singleAssetSharedState: SingleAssetSharedState
) : SingleChainUpdateSystem(chainRegistry, singleAssetSharedState) {

    override fun getUpdaters(chain: Chain, chainAsset: Chain.Asset): List<Updater> {
        return commonUpdaters + when (chainAsset.staking) {
            Chain.Asset.StakingType.UNSUPPORTED -> emptyList()
            Chain.Asset.StakingType.RELAYCHAIN -> relaychainUpdaters
            Chain.Asset.StakingType.PARACHAIN -> parachainUpdaters
        }
    }
}
