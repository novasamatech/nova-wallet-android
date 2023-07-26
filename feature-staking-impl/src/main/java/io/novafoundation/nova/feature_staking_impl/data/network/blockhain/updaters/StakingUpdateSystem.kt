package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.NOMINATION_POOLS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import io.novafoundation.nova.runtime.network.updaters.SingleChainUpdateSystem

class StakingUpdateSystem(
    private val relaychainUpdaters: List<Updater>,
    private val parachainUpdaters: List<Updater>,
    private val commonUpdaters: List<Updater>,
    private val turingExtraUpdaters: List<Updater>,
    private val nominationPoolsUpdaters: List<Updater>,
    chainRegistry: ChainRegistry,
    singleAssetSharedState: StakingSharedState,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : SingleChainUpdateSystem<StakingSharedState.OptionAdditionalData>(chainRegistry, singleAssetSharedState, storageSharedRequestsBuilderFactory) {

    override fun getUpdaters(selectedAssetOption: StakingOption): List<Updater> {
        return commonUpdaters + when (selectedAssetOption.additional.stakingType) {
            UNSUPPORTED -> emptyList()
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO -> relaychainUpdaters
            PARACHAIN -> parachainUpdaters
            TURING -> parachainUpdaters + turingExtraUpdaters
            NOMINATION_POOLS -> nominationPoolsUpdaters
        }
    }
}
