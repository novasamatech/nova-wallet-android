package io.novafoundation.nova.feature_staking_impl.data.common.network.blockhain.updaters

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

class TotalIssuanceUpdater(
    stakingSharedState: StakingSharedState,
    storageCache: StorageCache,
    chainRegistry: ChainRegistry
) : SingleStorageKeyUpdater<GlobalScope>(GlobalScope, stakingSharedState, chainRegistry, storageCache) {

    override val requiredModules: List<String> = listOf(Modules.BALANCES)

    override suspend fun storageKey(runtime: RuntimeSnapshot): String {
        return runtime.metadata.balances().storage("TotalIssuance").storageKey()
    }
}
