package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSession
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class CurrentEpochIndexUpdater(
    electionsSessionRegistry: ElectionsSessionRegistry,
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : ElectionsSessionParameterUpdater(electionsSessionRegistry, stakingSharedState, chainRegistry, storageCache) {

    override suspend fun ElectionsSession.updaterStorageKey(chainId: ChainId): String? {
        return currentEpochIndexStorageKey(chainId)
    }
}
