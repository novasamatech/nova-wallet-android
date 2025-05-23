package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater

import io.novafoundation.nova.common.utils.nominationPools
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.scope.PoolScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class PoolMetadataUpdater(
    poolScope: PoolScope,
    storageCache: StorageCache,
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
) : SingleStorageKeyUpdater<PoolId?>(poolScope, stakingSharedState, chainRegistry, storageCache), StakingUpdater<PoolId?> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: PoolId?): String? {
        if (scopeValue == null) return null

        return runtime.metadata.nominationPools().storage("Metadata").storageKey(runtime, scopeValue.value)
    }
}
