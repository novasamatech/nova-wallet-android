package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.defaultInHex
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.network.updaters.multiChain.SharedStateBasedUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull

class HistoryDepthUpdater(
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache,
) : SingleStorageKeyUpdater<Unit>(GlobalScope, stakingSharedState, chainRegistry, storageCache), SharedStateBasedUpdater<Unit> {

    override fun fallbackValue(runtime: RuntimeSnapshot): String? {
        return storageEntry(runtime)?.defaultInHex()
    }

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String? {
        return storageEntry(runtime)?.storageKey()
    }

    private fun storageEntry(runtime: RuntimeSnapshot) = runtime.metadata.staking().storageOrNull("HistoryDepth")
}
