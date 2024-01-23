package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.proxy
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.model.AccountStakingLocal
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.AccountStakingScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageOrNull

class ProxiesUpdater(
    scope: AccountStakingScope,
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : SingleStorageKeyUpdater<AccountStakingLocal>(scope, stakingSharedState, chainRegistry, storageCache), StakingUpdater<AccountStakingLocal> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: AccountStakingLocal): String? {
        val accountId = scopeValue.accountId
        return runtime.metadata.proxy().storageOrNull("Proxies")?.storageKey(runtime, accountId)
    }
}
