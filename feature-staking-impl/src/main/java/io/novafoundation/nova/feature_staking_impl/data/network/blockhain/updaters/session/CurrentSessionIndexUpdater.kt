package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_staking_impl.data.TimelineDelegatingChainIdHolder
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.currentIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.session
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.TimelineDelegatingSingleKeyUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.SharedStateBasedUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

class CurrentSessionIndexUpdater(
    timelineDelegatingChainIdHolder: TimelineDelegatingChainIdHolder,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : TimelineDelegatingSingleKeyUpdater<Unit>(GlobalScope, chainRegistry, storageCache, timelineDelegatingChainIdHolder), SharedStateBasedUpdater<Unit> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String {
        return runtime.provideContext {
            metadata.session.currentIndex.storageKey()
        }
    }
}
