package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class CurrentEraUpdater(
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache,
) : SingleStorageKeyUpdater<Unit>(GlobalScope, stakingSharedState, chainRegistry, storageCache), StakingUpdater<Unit> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String {
        return runtime.metadata.staking().storage("CurrentEra").storageKey()
    }
}
