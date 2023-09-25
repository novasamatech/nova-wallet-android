package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.turing

import io.novafoundation.nova.common.utils.automationTime
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.storage.insertPrefixEntries
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.ParachainStakingUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.withIndex

class TuringAutomationTasksUpdater(
    private val stakingSharedState: StakingSharedState,
    private val storageCache: StorageCache,
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    override val scope: AccountUpdateScope,
) : ParachainStakingUpdater<MetaAccount> {

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount
    ): Flow<Updater.SideEffect> {
        val chain = stakingSharedState.chain()
        val metaAccount = scopeValue
        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()
        val runtime = chainRegistry.getRuntime(chain.id)

        val accountKey = runtime.metadata.system().storage("Account").storageKey(runtime, accountId)

        return storageSubscriptionBuilder.subscribe(accountKey)
            .withIndex()
            .mapLatest { (index, change) ->
                val isChange = index > 0
                val at = if (isChange) change.block else null

                remoteStorageSource.query(chain.id, at) {
                    val storageEntry = runtime.metadata.automationTime().storage("AccountTasks")
                    val entries = storageEntry.entriesRaw(accountId)
                    val storagePrefix = storageEntry.storageKey(runtime, accountId)

                    storageCache.insertPrefixEntries(entries, prefix = storagePrefix, chainId = chain.id)
                }
            }.noSideAffects()
    }
}
