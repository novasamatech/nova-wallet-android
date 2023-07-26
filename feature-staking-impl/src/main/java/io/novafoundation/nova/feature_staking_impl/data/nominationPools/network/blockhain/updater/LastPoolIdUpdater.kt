package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater

import io.novafoundation.nova.common.utils.nominationPools
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.base.NominationPoolUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

class LastPoolIdUpdater(
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : SingleStorageKeyUpdater<GlobalScope>(GlobalScope, stakingSharedState, chainRegistry, storageCache),
    NominationPoolUpdater {

    override suspend fun storageKey(runtime: RuntimeSnapshot): String {
        return runtime.metadata.nominationPools().storage("LastPoolId").storageKey(runtime)
    }
}
