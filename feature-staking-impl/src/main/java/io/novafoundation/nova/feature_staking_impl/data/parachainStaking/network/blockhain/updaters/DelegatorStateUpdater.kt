package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class DelegatorStateUpdater(
    scope: AccountUpdateScope,
    storageCache: StorageCache,
    val stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
) : SingleStorageKeyUpdater<MetaAccount>(scope, stakingSharedState, chainRegistry, storageCache), StakingUpdater<MetaAccount> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: MetaAccount): String? {
        val account = scopeValue
        val chain = stakingSharedState.chain()

        val accountId = account.accountIdIn(chain) ?: return null

        return runtime.metadata.parachainStaking().storage("DelegatorState").storageKey(runtime, accountId)
    }
}
