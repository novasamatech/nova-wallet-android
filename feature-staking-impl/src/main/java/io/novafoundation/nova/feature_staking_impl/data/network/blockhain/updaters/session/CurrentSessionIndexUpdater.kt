package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.common.utils.session
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.TimelineDelegatingChainIdHolder
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.currentIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.session
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.TimelineDelegatingSingleKeyUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class CurrentSessionIndexUpdater(
    timelineDelegatingChainIdHolder: TimelineDelegatingChainIdHolder,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : TimelineDelegatingSingleKeyUpdater<Unit>(GlobalScope, chainRegistry, storageCache, timelineDelegatingChainIdHolder), StakingUpdater<Unit> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String {
        return runtime.provideContext {
            metadata.session.currentIndex.storageKey()
        }
    }
}
