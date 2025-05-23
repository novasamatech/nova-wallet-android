package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_staking_impl.data.TimelineDelegatingChainIdHolder
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater

abstract class TimelineDelegatingSingleKeyUpdater<V>(
    scope: UpdateScope<V>,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache,
    timelineDelegatingChainIdHolder: TimelineDelegatingChainIdHolder
): SingleStorageKeyUpdater<V>(scope, timelineDelegatingChainIdHolder, chainRegistry, storageCache), StakingUpdater<V> {

    override fun getSyncChainId(stakingChain: Chain): ChainId {
        return stakingChain.timelineChainIdOrSelf()
    }
}
