package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import io.novafoundation.nova.runtime.network.updaters.SingleChainUpdateSystem
import io.novafoundation.nova.runtime.state.SingleAssetSharedState

class StakingUpdateSystem(
    private val relaychainUpdaters: List<Updater>,
    private val parachainUpdaters: List<Updater>,
    private val commonUpdaters: List<Updater>,
    private val turingExtraUpdaters: List<Updater>,
    chainRegistry: ChainRegistry,
    singleAssetSharedState: SingleAssetSharedState
) : SingleChainUpdateSystem(chainRegistry, singleAssetSharedState) {

    override fun getUpdaters(chain: Chain, chainAsset: Chain.Asset): List<Updater> {
        return commonUpdaters + when (chainAsset.staking) {
            UNSUPPORTED -> emptyList()
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO -> relaychainUpdaters
            PARACHAIN -> parachainUpdaters
            TURING -> parachainUpdaters + turingExtraUpdaters
        }
    }
}
