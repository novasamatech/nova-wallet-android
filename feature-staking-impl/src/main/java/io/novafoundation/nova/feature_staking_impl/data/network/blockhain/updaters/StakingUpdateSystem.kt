package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.multiChain.MultiChainUpdateSystem


class StakingUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val stakingUpdaters: StakingUpdaters,
    stakingSharedState: StakingSharedState,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : MultiChainUpdateSystem<StakingSharedState.OptionAdditionalData>(chainRegistry, stakingSharedState, storageSharedRequestsBuilderFactory) {

    override fun getUpdaters(option: StakingOption): MultiMap<ChainId, Updater<*>> {
        return stakingUpdaters.getUpdaters(option.chain, option.stakingType)
    }
}
