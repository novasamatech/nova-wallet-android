package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.TimelineDelegatingChainIdHolder
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSession
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class CurrentSlotUpdater(
    electionsSessionRegistry: ElectionsSessionRegistry,
    timelineDelegatingChainIdHolder: TimelineDelegatingChainIdHolder,
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : ElectionsSessionParameterUpdater(
    electionsSessionRegistry = electionsSessionRegistry,
    stakingSharedState = stakingSharedState,
    timelineDelegatingChainIdHolder = timelineDelegatingChainIdHolder,
    chainRegistry = chainRegistry,
    storageCache = storageCache
) {

    override suspend fun ElectionsSession.updaterStorageKey(chainId: ChainId): String? {
        return currentSlotStorageKey(chainId)
    }
}
