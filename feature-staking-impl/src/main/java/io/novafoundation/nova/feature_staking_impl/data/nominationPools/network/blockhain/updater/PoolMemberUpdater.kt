package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater

import io.novafoundation.nova.common.utils.nominationPools
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.base.NominationPoolUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

class PoolMemberUpdater(
    scope: AccountUpdateScope,
    storageCache: StorageCache,
    val stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
) : SingleStorageKeyUpdater<AccountUpdateScope>(scope, stakingSharedState, chainRegistry, storageCache), NominationPoolUpdater {

    override suspend fun storageKey(runtime: RuntimeSnapshot): String? {
        val account = scope.getAccount()
        val chain = stakingSharedState.chain()

        val accountId = account.accountIdIn(chain) ?: return null

        return runtime.metadata.nominationPools().storage("PoolMembers").storageKey(runtime, accountId)
    }
}
