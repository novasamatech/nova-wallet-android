package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleChainUpdateSystem

class StakingUpdateSystem(
    private val stakingUpdaters: StakingUpdaters,
    chainRegistry: ChainRegistry,
    singleAssetSharedState: StakingSharedState,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : SingleChainUpdateSystem<StakingSharedState.OptionAdditionalData>(chainRegistry, singleAssetSharedState, storageSharedRequestsBuilderFactory) {

    override fun getUpdaters(selectedAssetOption: StakingOption): Collection<Updater<*>> {
        return stakingUpdaters.getUpdaters(selectedAssetOption.additional.stakingType)
    }
}
