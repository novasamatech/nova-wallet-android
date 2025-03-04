package io.novafoundation.nova.feature_staking_impl.data.mythos.updaters

import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.releaseQueues
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

class MythosReleaseQueuesUpdater(
    private val stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache,
    scope: AccountUpdateScope,
) : SingleStorageKeyUpdater<MetaAccount>(scope, stakingSharedState, chainRegistry, storageCache) {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: MetaAccount): String? {
        return with(RuntimeContext(runtime)) {
            val chain = stakingSharedState.chain()
            val accountId = scopeValue.accountIdIn(chain) ?: return@with null

            metadata.collatorStaking.releaseQueues.storageKey(accountId)
        }
    }
}
